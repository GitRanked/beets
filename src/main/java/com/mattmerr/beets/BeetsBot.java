package com.mattmerr.beets;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.mattmerr.beets.commands.Command;
import com.mattmerr.beets.commands.CommandManager;
import com.mattmerr.beets.data.ClipManager;
import com.mattmerr.beets.data.SqliteModule;
import com.mattmerr.beets.util.CachedBeetLoader;
import com.mattmerr.beets.util.RepliableEventException;
import com.mattmerr.beets.vc.VCManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.MessageInteraction;
import discord4j.core.object.entity.User;
import discord4j.rest.RestClient;
import discord4j.rest.util.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static com.mattmerr.beets.util.UtilD4J.simpleMessageEmbed;
import static com.mattmerr.beets.util.UtilD4J.wrapEmbedReplyEphemeral;

public class BeetsBot {

  public static final Color COLOR = Color.of(0xFF4444);
  private static final Logger log = LoggerFactory.getLogger(BeetsBot.class);

  private static final Map<String, Command> commands = new HashMap<>();

  private static Injector injector;
  private final GatewayDiscordClient client;
  private final CachedBeetLoader beetLoader;
  private final ClipManager clipManager;
  private final VCManager vcManager;
  private final AllowedChannelResolver allowed;
  private final CommandManager cmd;

  @Inject
  BeetsBot(GatewayDiscordClient client, String[] args) {
    this.client = client;
    AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    playerManager.getConfiguration()
        .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
    AudioSourceManagers.registerRemoteSources(playerManager);
    this.beetLoader = new CachedBeetLoader(playerManager);
    this.vcManager = new VCManager(client, playerManager, beetLoader);

    var sqliteMod =
        new SqliteModule(Path.of(args.length > 1 ? args[1] : "beets.sqlite"));
    this.clipManager = new ClipManager(sqliteMod.conn);
    this.allowed = new AllowedChannelResolver(client);
    this.cmd = new CommandManager(vcManager, clipManager, beetLoader);
  }

  void onSlashCommand(@Nonnull SlashCommandEvent event) {
    log.info("Received event!");
    String channelId = event.getInteraction().getChannelId().asString();
    Optional<String> guildId =
        event.getInteraction().getGuildId().map(Snowflake::asString);
    if (guildId.isPresent()
        && allowed.restrictedGuilds.contains(guildId.get())
        && !allowed.allowedChannels.contains(channelId)) {
      log.info("Oops! Someone tried using Beets in wrong channel.");
      event.reply(wrapEmbedReplyEphemeral(
              simpleMessageEmbed("Wrong Channel!", "Please use Beets in #bots")))
          .block();
      return;
    }
    log.info("Command: " + event.getCommandName());
    try {
      var handler = cmd.commandsByName.get(event.getCommandName());
      handler.execute(event).doOnError(e -> {
        if (!(e instanceof RepliableEventException)) {
          log.warn("Error running guild command", e);
        }
      }).onErrorResume(e -> {
        if (e instanceof RepliableEventException) {
          return ((RepliableEventException) e).replyToEvent(event);
        }
        return Mono.empty();
      }).block();
    } catch (Exception err) {
      err.printStackTrace();
    }
  }

  void onMessageUpdate(MessageUpdateEvent event) {
    var eventAuthor = event.getOld()
        .flatMap(oldMsg -> oldMsg.getAuthor())
        .map(author -> author.getId().asString());

    if (!(eventAuthor.isPresent() &&
              (eventAuthor.get().equals("944401438233202728") ||
                   eventAuthor.get().equals("477651428379197469")))) {
      return;
    }
    event.getMessage().flatMap(msg -> {
      if (!msg.getEmbeds()
          .stream()
          .anyMatch(embed -> embed.getDescription()
              .orElse("")
              .contains("Winner winner chicken dinner!"))) {
        return Mono.empty();
      }

      var winner = msg.getInteraction()
          .map(MessageInteraction::getUser)
          .map(User::getId);

      if (winner.isEmpty()) {
        return Mono.empty();
      }

      return event.getGuild()
          .flatMap(guild -> guild.getMemberById(winner.get()))
          .flatMap(winnerMember -> winnerMember.getVoiceState())
          .flatMap(voiceState -> voiceState.getChannel())
          .flatMap(vc -> vcManager.interject(null, vc,
                                             "https://www.youtube.com/watch?v=TDJUot9OBLc"));
    }).block();
  }

  void onButtonInteract(ButtonInteractEvent event) {
    log.info("Received button: {}", event.getCustomId());
    try {
      var handler = cmd.buttonsByName.get(event.getCustomId());
      handler.execute(event).doOnError(e -> {
        if (!(e instanceof RepliableEventException)) {
          log.warn("Error responding to button interact", e);
        }
      }).onErrorResume(e -> {
        if (e instanceof RepliableEventException) {
          return ((RepliableEventException) e).replyToEvent(event);
        }
        return Mono.empty();
      }).block();
    } catch (Exception err) {
      log.warn("Error running button command", err);
    }
  }

  public static void main(String[] args) throws IOException {
    try {
      log.info("ArgsLen: " + args.length);
      if (Files.exists(Path.of(args[0]))) {
        args[0] = Files.readString(Path.of(args[0]));
      }
      args[0] = args[0].strip();
      if (args.length == 1) {
        log.info("Token: " + URLEncoder.encode(args[0]));
      }

      final GatewayDiscordClient client =
          DiscordClientBuilder.create(args[0]).build().login().block();
      var beets = new BeetsBot(client, args);
      var restClient = client.getRestClient();
      var applicationId = restClient.getApplicationId().block();

      client.on(SlashCommandEvent.class)
          .subscribe(inFiber(beets::onSlashCommand));
      client.on(MessageUpdateEvent.class)
          .subscribe(inFiber(beets::onMessageUpdate));
      client.on(ButtonInteractEvent.class)
          .subscribe(inFiber(beets::onButtonInteract));

      List<Thread> overwriteThreads = new ArrayList<>();
      for (var guild : client.getGuilds().collectList().block()) {
        overwriteThreads.add(
            Thread.startVirtualThread(() -> {
              restClient.getApplicationService()
                  .bulkOverwriteGuildApplicationCommand(applicationId,
                                                        guild.getId()
                                                            .asLong(),
                                                        beets.cmd.commandRequests())
                  .doOnError(
                      e -> log.warn("Unable to create guild command", e))
                  .onErrorResume(e -> Mono.empty())
                  .last();
            }));
      }
      for (var thread : overwriteThreads) {
        thread.join();
      }
      client.onDisconnect().block();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  static <T> Consumer<T> inFiber(Consumer<T> consumer) {
    return (T t) -> Thread.startVirtualThread(() -> consumer.accept(t));
  }


}
