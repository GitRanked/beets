package com.mattmerr.beets.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.data.ClipManager;
import com.mattmerr.beets.util.CachedBeetLoader;
import com.mattmerr.beets.vc.VCManager;
import com.mattmerr.beets.vc.VCSession;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.Duration;
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
    String guildId = requireGuildId(event.getInteraction()).asString();
    var delayMinutes = asLong(event.getOption("delay"));
    if (delayMinutes.isPresent() && (delayMinutes.get() < 1 || delayMinutes.get() > 120)) {
      event.reply("Delay must be in range [1, 120] minutes");
    }
    if (VALID_CLIP_NAME.matcher(beet.toLowerCase(Locale.ROOT)).matches()) {
      try {
        var clipOp = clipMgr.selectClip(guildId, beet.toLowerCase(Locale.ROOT));
        if (clipOp.isPresent()) {
          beet = clipOp.get().beet();
        }
      } catch (SQLException e) {
        log.error("Error looking up beet by name", e);
      }
    }
    AudioTrack track = beetLoader.getTrack(beet);
    VCSession session = vcManager.findOrCreateSession(event);
    if (delayMinutes.isPresent()) {
      Thread.ofVirtual().start(() -> {
        try {
          Thread.sleep(Duration.ofMinutes(delayMinutes.get()));
          session.connect();
          session.getTrackScheduler().interject(track);
        } catch (InterruptedException e) {
          log.error("Who woke me up I was sleeping", e);
        }
      });
      return event.reply(format("Will wait %d minutes and play: %s", delayMinutes.get(), beet));
    }
    session.connect();
    if (!session.getTrackScheduler().enqueue(track)) {
      return event.reply("Queue is full! Cannot play.");
    }
    if (session.getTrackScheduler().getQueue().isEmpty()) {
      return event.reply("Now playing: " + beet);
    }
    return event.reply("Added to queue: " + beet);
  }

}
