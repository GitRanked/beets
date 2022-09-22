package com.mattmerr.beets.commands;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mattmerr.beets.data.ClipManager;
import com.mattmerr.beets.util.CachedBeetLoader;
import com.mattmerr.beets.vc.VCManager;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {

  private static final Logger log = LoggerFactory.getLogger(CommandManager.class);

  public final ImmutableList<Command> handlers;
  public final ImmutableMap<String, Command> commandsByName;
  public final ImmutableMap<String, ButtonCommand> buttonsByName;
  public final ImmutableList<ApplicationCommandRequest> commandRequests;
  public final DiscordClient client;

  public CommandManager(GatewayDiscordClient client,
                        VCManager vcManager,
                        ClipManager clipManager,
                        CachedBeetLoader beetLoader) {
    this.client = client.rest();
    this.handlers = ImmutableList.of(
        new PingCommand(),
        new PlayCommand(vcManager, clipManager, beetLoader),
        new QueueCommand(vcManager),
        new SkipCommand(vcManager),
        new StopCommand(vcManager),
        new ClipCommand(clipManager, beetLoader),
        new ClipListCommand(clipManager),
        new PuntCommand(vcManager),
        new PromoteCommand(vcManager));

    ImmutableMap.Builder<String, Command> cmdMapBuilder = ImmutableMap.builder();
    ImmutableMap.Builder<String, ButtonCommand> btnMapBuilder = ImmutableMap.builder();
    for (var handler : handlers) {
      var desc = handler.getClass().getAnnotation(CommandDesc.class);
      if (desc == null) {
        log.error("No CommandDesc on " + handler.getClass().getName());
        continue;
      }
      cmdMapBuilder.put(desc.name(), handler);

      if (handler instanceof ButtonCommand) {
        btnMapBuilder.put("cmd:" + desc.name(),
                          (ButtonCommand) handler);
      }
    }
    this.commandsByName = cmdMapBuilder.build();
    this.buttonsByName = btnMapBuilder.build();
    this.commandRequests = convertRequests();
  }

  private static ApplicationCommandRequest convertDescription(CommandDesc desc) {
    var requestBuilder = ApplicationCommandRequest.builder()
        .name(desc.name())
        .description(desc.description());

    for (CommandDesc.Option option : desc.options()) {
      requestBuilder.addOption(convertOption(option));
    }
    return requestBuilder.build();
  }

  private static ApplicationCommandOptionData convertOption(CommandDesc.Option option) {
    return ApplicationCommandOptionData.builder()
        .name(option.name())
        .description(option.description())
        .type(option.type().getValue())
        .required(option.required())
        .build();
  }

  public ImmutableList<ApplicationCommandRequest> convertRequests() {
    ImmutableList.Builder<ApplicationCommandRequest> commandsBuilder = ImmutableList.builder();
    for (var handler : handlers) {
      var desc = handler.getClass().getAnnotation(CommandDesc.class);
      if (desc == null) {
        log.error("No CommandDesc on " + handler.getClass().getName());
        continue;
      }
      commandsBuilder.add(convertDescription(desc));
    }
    return commandsBuilder.build();
  }

  public void register() {
    long appId = this.client.getApplicationId().block();
    List<Thread> fibers = new ArrayList<>();
    for (var guild : client.getGuilds().collectList().block()) {
      fibers.add(
          Thread.startVirtualThread(
              () -> registerInGuild(appId, guild.id().asLong())));
    }
    try {
      for (Thread fiber : fibers) {
        fiber.join();
      }
    } catch (InterruptedException e) {
      log.error("Interrupted registering guild commads", e);
    }
  }

  public void registerInGuild(long appId, long guildId) {
    this.client.getApplicationService()
        .bulkOverwriteGuildApplicationCommand(appId,
                                              guildId,
                                              commandRequests)
        .doOnError(e -> log.warn("Unable to create guild command", e))
        .onErrorResume(e -> Mono.empty())
        .blockLast();
  }
}
