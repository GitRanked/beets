package com.mattmerr.beets;

import com.mattmerr.beets.commands.Command;
import com.mattmerr.beets.commands.CommandManager;
import com.mattmerr.beets.data.ClipManager;
import com.mattmerr.beets.data.SqliteModule;
import com.mattmerr.beets.util.CachedBeetLoader;
import com.mattmerr.beets.util.RepliableEventException;
import com.mattmerr.beets.util.RepliableEventException.MissingGuildException;
import com.mattmerr.beets.vc.VCManager;
import com.mattmerr.beets.vc.VCSession;
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
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.PartialMember;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.mattmerr.beets.util.UtilD4J.simpleMessageEmbed;
import static com.mattmerr.beets.util.UtilD4J.wrapEmbedReplyEphemeral;

public class BeetsBot {

  public static final Color COLOR = Color.of(0xFF4444);
  private static final Logger log = LoggerFactory.getLogger(BeetsBot.class);

  private static final Map<String, Command> commands = new HashMap<>();

  private final CachedBeetLoader beetLoader;
  private final ClipManager clipManager;
  private final VCManager vcManager;
  private final AllowedChannelResolver allowed;
  private final CommandManager cmd;
  private final long applicationId;

  BeetsBot(GatewayDiscordClient client, String[] args) {
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
    this.cmd = new CommandManager(client, vcManager, clipManager, beetLoader);
    this.applicationId = client.rest().getApplicationId().block();
  }

  void onMessageUpdate(MessageUpdateEvent event) {
    var eventAuthor = event.getOld()
        .flatMap(Message::getAuthor)
        .map(author -> author.getId().asString());

    if (!(eventAuthor.isPresent() &&
              (eventAuthor.get().equals("944401438233202728") ||
                   eventAuthor.get().equals("477651428379197469")))) {
      return;
    }
    Message msg = event.getMessage().block();
    if (!msg.getEmbeds()
        .stream()
        .anyMatch(embed -> embed.getDescription()
            .orElse("")
            .contains("Winner winner chicken dinner!"))) {
      return;
    }
    var winner = msg.getInteraction()
        .map(MessageInteraction::getUser)
        .map(User::getId);

    if (winner.isEmpty()) {
      return;
    }
    event.getGuild()
        .flatMap(guild -> guild.getMemberById(winner.get()))
        .map(member -> {
          VCSession session = vcManager.findOrCreateSession(member);
          session.connect();
          session.getTrackScheduler().interject(
              beetLoader.getTrack(
                  "https://www.youtube.com/watch?v=TDJUot9OBLc"));
          log.info("Detected a winner!");
          return true;
        })
        .block();
  }

  void onSlashCommand(@Nonnull SlashCommandEvent event) {
    try {
      log.debug("received SlashCommandEvent");
      String channelId = event.getInteraction().getChannelId().asString();
      String guildId =
          event.getInteraction().getGuildId().map(Snowflake::asString)
              .orElseThrow(MissingGuildException::new);
      if (allowed.restrictedGuilds.contains(guildId)
          && !allowed.allowedChannels.contains(channelId)) {
        log.debug("discarding invalid channel for SlashCommandEvent");
        event.reply(wrapEmbedReplyEphemeral(
                simpleMessageEmbed("Wrong Channel!",
                                   "You may not use beets in this channel.")))
            .block();
        return;
      }
      log.info("Command: {}", event.getCommandName());
      var handler = cmd.commandsByName.get(event.getCommandName());
      handler.execute(event).block();
    } catch (RepliableEventException e) {
      e.replyToEvent(event).block();
    } catch (Exception err) {
      log.warn("Error running button command", err);
    }
  }

  void onButtonInteract(ButtonInteractEvent event) {
    log.info("Received button: {}", event.getCustomId());
    var handler = cmd.buttonsByName.get(event.getCustomId());
    try {
      handler.execute(event).block();
    } catch (RepliableEventException e) {
      e.replyToEvent(event).block();
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

      client.on(SlashCommandEvent.class)
          .subscribe(inFiber(beets::onSlashCommand));
      client.on(MessageUpdateEvent.class)
          .subscribe(inFiber(beets::onMessageUpdate));
      client.on(ButtonInteractEvent.class)
          .subscribe(inFiber(beets::onButtonInteract));

      Thread.ofVirtual()
          .name("beets-cmd-register")
          .start(beets.cmd::register)
          .join();

      client.onDisconnect().block();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  static <T> Consumer<T> inFiber(Consumer<T> consumer) {
    return (T t) -> Thread.ofVirtual()
        .name("beets-cmd-", 0)
        .start(() -> consumer.accept(t));
  }


}
