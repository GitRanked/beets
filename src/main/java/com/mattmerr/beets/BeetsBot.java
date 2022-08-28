package com.mattmerr.beets;

import static com.mattmerr.beets.util.UtilD4J.simpleMessageEmbed;
import static com.mattmerr.beets.util.UtilD4J.wrapEmbedReply;
import static com.mattmerr.beets.util.UtilD4J.wrapEmbedReplyEphemeral;
import static java.lang.String.format;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.mattmerr.beets.commands.Command;
import com.mattmerr.beets.commands.PlayCommand;
import com.mattmerr.beets.data.SqliteModule;
import com.mattmerr.beets.util.RepliableEventException;
import com.mattmerr.beets.vc.VCManager;
import com.mattmerr.beets.vc.VoiceModule;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ButtonInteractEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.MessageInteraction;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.rest.RestClient;
import discord4j.rest.util.Color;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class BeetsBot {

  public static final Color COLOR = Color.of(0xFF4444);
  private static final Logger log = LoggerFactory.getLogger(BeetsBot.class);

  private static final Map<String, Command> commands = new HashMap<>();

  public static void main(String[] args) throws IOException {
    log.info("ArgsLen: " + args.length);
    if (Files.exists(Path.of(args[0]))) {
      args[0] = Files.readString(Path.of(args[0]));
    }
    args[0] = args[0].strip();
    if (args.length == 1) {
      log.info("Token: " + URLEncoder.encode(args[0]));
    }

    Injector injector =
        Guice.createInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                super.configure();
                final GatewayDiscordClient client =
                    DiscordClientBuilder.create(args[0]).build().login().block();
                bind(GatewayDiscordClient.class).toInstance(client);
                bind(RestClient.class).toInstance(client.getRestClient());

                CommandManager commandManager = new CommandManager();
                bind(CommandManager.class).toInstance(commandManager);

                install(new VoiceModule());
                install(new SqliteModule(Path.of(args.length > 1 ? args[1] : "beets.sqlite")));
              }
            });
    injector.getInstance(BeetsBot.class);

    var client = injector.getInstance(GatewayDiscordClient.class);
    var restClient = injector.getInstance(RestClient.class);
    var commandManager = injector.getInstance(CommandManager.class);
    long applicationId = restClient.getApplicationId().block();

    log.info("weee");

    //    client.getMessageById(Snowflake.of("channelid"), Snowflake.of("msgid"))
    //        .flatMap(msg -> msg.delete("(hardcoded) Wrong channel!"))
    //        .subscribe(vd -> log.info("Deleted that bad message!"));

    client
        .getGuilds()
        .map(
            guild -> {
              restClient
                  .getApplicationService()
                  .bulkOverwriteGuildApplicationCommand(
                      applicationId, guild.getId().asLong(), commandManager.commandRequests())
                  .doOnError(e -> log.warn("Unable to create guild command", e))
                  .onErrorResume(e -> Mono.empty())
                  .blockLast();
              return guild.getId();
            })
        .blockLast();

    String allowedChannel = System.getenv("BEETS_ALLOWED_CHANNEL");
    Set<String> allowedChannels;
    if (allowedChannel == null || allowedChannel.isEmpty()) {
      log.warn("NO BEETS_ALLOWED_CHANNEL DEFINED! NO BEETS_ALLOWED_CHANNEL DEFINED!");
      log.warn("NO BEETS_ALLOWED_CHANNEL DEFINED! NO BEETS_ALLOWED_CHANNEL DEFINED!");
      log.warn("NO BEETS_ALLOWED_CHANNEL DEFINED! NO BEETS_ALLOWED_CHANNEL DEFINED!");
      allowedChannels = null;
    } else {
      allowedChannels = Set.of(allowedChannel.split(","));
    }

    client
        .on(
            new ReactiveEventAdapter() {

              @Nonnull
              @Override
              public Publisher<?> onSlashCommand(@Nonnull SlashCommandEvent event) {
                log.info("Received event!");
                String channelId = event.getInteraction().getChannelId().asString();
                if (allowedChannels != null && !allowedChannels.contains(channelId)) {
                  log.info("Oops! Someone tried using Beets in wrong channel.");
                  return event.reply(
                      wrapEmbedReplyEphemeral(
                          simpleMessageEmbed("Wrong Channel!", "Please use Beets in #bots")));
                }
                log.info("Command: " + event.getCommandName());
                try {
                  var cmdClass = CommandManager.commandsByName.get(event.getCommandName());
                  return injector
                      .getInstance(cmdClass)
                      .execute(event)
                      .doOnError(
                          e -> {
                            if (!(e instanceof RepliableEventException)) {
                              log.warn("Error running guild command", e);
                            }
                          })
                      .onErrorResume(
                          e -> {
                            if (e instanceof RepliableEventException) {
                              return ((RepliableEventException) e).replyToEvent(event);
                            }
                            return Mono.empty();
                          });
                } catch (Exception err) {
                  err.printStackTrace();
                  return Mono.empty();
                }
              }
            })
        .doOnError(e -> log.warn("Error running guild command", e))
        .subscribe();

    client
        .on(
            new ReactiveEventAdapter() {

              @Override
              public Publisher<?> onMessageUpdate(MessageUpdateEvent event) {
                var eventAuthor =
                    event
                        .getOld()
                        .flatMap(oldMsg -> oldMsg.getAuthor())
                        .map(author -> author.getId().asString());

                if (!(eventAuthor.isPresent()
                    && (eventAuthor.get().equals("944401438233202728")
                        || eventAuthor.get().equals("477651428379197469")))) {
                  return Mono.empty();
                }
                return event
                    .getMessage()
                    .flatMap(
                        msg -> {
                          if (!msg.getEmbeds().stream()
                              .anyMatch(
                                  embed ->
                                      embed
                                          .getDescription()
                                          .orElse("")
                                          .contains("Winner winner chicken dinner!"))) {
                            return Mono.empty();
                          }

                          VCManager vcManager = injector.getInstance(VCManager.class);
                          var winner =
                              msg.getInteraction()
                                  .map(MessageInteraction::getUser)
                                  .map(User::getId);

                          if (winner.isEmpty()) {
                            return Mono.empty();
                          }

                          return event
                              .getGuild()
                              .flatMap(guild -> guild.getMemberById(winner.get()))
                              .flatMap(winnerMember -> winnerMember.getVoiceState())
                              .flatMap(voiceState -> voiceState.getChannel())
                              .flatMap(
                                  vc ->
                                      vcManager.interject(
                                          null, vc, "https://www.youtube.com/watch?v=TDJUot9OBLc"));
                        });
              }
            })
        .doOnError(e -> log.warn("Error stalking chickens", e))
        .subscribe();

    client
        .on(
            ButtonInteractEvent.class,
            event -> {
              log.info("Received button: {}", event.getCustomId());
              try {
                var cmdClass = CommandManager.buttonsByName.get(event.getCustomId());
                return injector
                    .getInstance(cmdClass)
                    .execute(event)
                    .doOnError(
                        e -> {
                          if (!(e instanceof RepliableEventException)) {
                            log.warn("Error running guild command", e);
                          }
                        })
                    .onErrorResume(
                        e -> {
                          if (e instanceof RepliableEventException) {
                            return ((RepliableEventException) e).replyToEvent(event);
                          }
                          return Mono.empty();
                        });
              } catch (Exception err) {
                err.printStackTrace();
                return Mono.empty();
              }
            })
        .doOnError(e -> log.warn("Error running button command", e))
        .subscribe();

    client.onDisconnect().block();
  }

  @Inject
  BeetsBot() {}
}
