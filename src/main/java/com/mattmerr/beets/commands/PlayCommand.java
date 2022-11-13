package com.mattmerr.beets.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.data.ClipManager;
import com.mattmerr.beets.util.CachedBeetLoader;
import com.mattmerr.beets.util.RepliableMessageException;
import com.mattmerr.beets.vc.VCManager;
import com.mattmerr.beets.vc.VCSession;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import static com.mattmerr.beets.data.Clip.VALID_CLIP_NAME;
import static com.mattmerr.beets.util.UtilD4J.*;
import static java.lang.String.format;

@CommandDesc(
    name = "play",
    description = "Pick a beet",
    options = {
        @CommandDesc.Option(
            name = "beet",
            description = "where's the beet?",
            type = ApplicationCommandOptionType.STRING,
            required = true),
        @CommandDesc.Option(
            name = "multiqueue",
            description = "queue all beets from a playlist?",
            type = ApplicationCommandOptionType.BOOLEAN,
            required = false),
        @CommandDesc.Option(
            name = "delay",
            description = "delays playback, in minutes",
            type = ApplicationCommandOptionType.INTEGER,
            required = false),
    }
)
@Singleton
public class PlayCommand extends CommandBase {

  private final VCManager vcManager;
  private final ClipManager clipMgr;
  private final CachedBeetLoader beetLoader;

  @Inject
  PlayCommand(VCManager vcManager, ClipManager clipMgr, CachedBeetLoader beetLoader) {
    this.vcManager = vcManager;
    this.clipMgr = clipMgr;
    this.beetLoader = beetLoader;
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
    logCall(event);
    String beet = asRequiredString(event.getOption("beet"));
    Snowflake guildId = requireGuildId(event.getInteraction());

    var multiqueue = asBoolean(event.getOption("multiqueue")).orElse(false);
    var delayMinutes = asLong(event.getOption("delay"));
    if (delayMinutes.isPresent()) {
      if (delayMinutes.get() < 1 || delayMinutes.get() > 120) {
        return event.reply("Delay must be in range [1, 120] minutes");
      } else if (multiqueue) {
        return event.reply("You cannot enable multiqueue and delay at the same time");
      }
    }

    if (VALID_CLIP_NAME.matcher(beet.toLowerCase(Locale.ROOT)).matches()) {
      try {
        var clipOp = clipMgr.selectClip(guildId.asString(),
                                        beet.toLowerCase(Locale.ROOT));
        if (clipOp.isPresent()) {
          beet = clipOp.get().beet();
        }
      } catch (SQLException e) {
        log.error("Error looking up beet by name", e);
      }
    }

    List<AudioTrack> tracks = beetLoader.getTracks(beet, multiqueue);
    final var beetCapture = beet;
    if (delayMinutes.isPresent()) {
      AudioTrack track = tracks.iterator().next(); // Cannot multiqueue and delay, so this will always have 1 element.
      Thread.ofVirtual()
          .start(() -> {
            try {
              Thread.sleep(Duration.ofMinutes(delayMinutes.get()));
              VCSession session = vcManager.findOrCreateSession(event);
              session.connect();
              session.getTrackScheduler().interject(track);
              // Interaction tokens are only valid for 15 minutes
              if (delayMinutes.get() < 15) {
                event.getInteractionResponse()
                    .createFollowupMessage("Now playing: " + beetCapture)
                    .block();
              }
            } catch (InterruptedException e) {
              log.error("Who woke me up I was sleeping", e);
            } catch (RepliableMessageException e) {
              event.getInteractionResponse()
                  .createFollowupMessage(e.getReplyMessage())
                  .block();
            }
            log.debug("Completed PlayCommand delayed by {} minutes",
                      delayMinutes.get());
          })
          .setUncaughtExceptionHandler(
              (t, e) -> {
                log.error("Uncaught error from PlayCommand delay fiber", e);
                event.getInteractionResponse()
                    .createFollowupMessage("There was an error playing your delayed beet :(")
                    .block();
              });
      return event.reply(format("Will wait %d minutes and play: %s", delayMinutes.get(), beet));
    }

    VCSession session = vcManager.findOrCreateSession(event);
    session.connect();
    for (int i = 0; i < tracks.size(); i++) {
      AudioTrack track = tracks.get(i);
      if (!session.getTrackScheduler().enqueue(track)) {
        if (i == 0) {
          return event.reply("Queue is full! Cannot add any new tracks.");
        } else {
          return event.reply("Queue is now full! Could not add all tracks in playlist.");
        }
      }
    }

    if (session.getTrackScheduler().getQueue().isEmpty()) {
      return event.reply("Now playing: " + beet);
    }
    return event.reply("Added to queue: " + beet);
  }
}
