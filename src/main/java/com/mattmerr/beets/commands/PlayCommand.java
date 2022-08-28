package com.mattmerr.beets.commands;

import static com.mattmerr.beets.data.Clip.VALID_CLIP_NAME;
import static com.mattmerr.beets.util.UtilD4J.asLong;
import static com.mattmerr.beets.util.UtilD4J.asRequiredString;
import static com.mattmerr.beets.util.UtilD4J.requireGuildId;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.data.ClipManager;
import com.mattmerr.beets.vc.VCManager;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.rest.util.ApplicationCommandOptionType;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Locale;
import reactor.core.publisher.Mono;

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

  @Inject
  PlayCommand(VCManager vcManager, ClipManager clipMgr) {
    this.vcManager = vcManager;
    this.clipMgr = clipMgr;
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
        log.debug("error looking up beet", e);
      }
    }

    final String beetCapture = beet;
    
    return vcManager
        .getChannelForInteraction(event.getInteraction())
        .flatMap(vc -> {
          if (delayMinutes.isPresent()) {
            Mono.delay(Duration.ofMinutes(delayMinutes.get()))
                .flatMap(zero -> {
                  return vcManager.interject(event, vc, beetCapture);
                })
                .doOnError(err -> log.error("Error playing after delay:", err))
                .subscribe(done -> event.getInteractionResponse().createFollowupMessage("Played the thing!"));
            return event.reply(format("Will wait %d minutes and play: %s", delayMinutes.get(), beetCapture));
          }
          return vcManager.enqueue(event, vc, beetCapture);
        })
        .doOnError(e -> log.error("Error processing Play", e))
        .onErrorResume(e -> event.reply("Error trying to Play!"));
  }

}
