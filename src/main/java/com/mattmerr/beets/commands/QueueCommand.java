package com.mattmerr.beets.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.vc.VCManager;
import com.mattmerr.beets.vc.VCSession;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.StringJoiner;

import static com.mattmerr.beets.util.UtilD4J.simpleMessageEmbed;
import static java.lang.String.format;

@CommandDesc(
    name = "queue",
    description = "queue the beets",
    options = {}
)
@Singleton
public class QueueCommand extends CommandBase {

  private final VCManager vcManager;

  @Inject
  QueueCommand(VCManager vcManager) {
    this.vcManager = vcManager;
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
    logCall(event);
    return event.reply(responseForSession(vcManager.peekSession(event)));
  }
  
  public static InteractionApplicationCommandCallbackSpec responseForSession(VCSession session) {
    if (session == null) {
      return InteractionApplicationCommandCallbackSpec.builder()
          .addEmbed(simpleMessageEmbed("Beets Queue", "Queue up some beets!"))
          .build();
    }
    
    var status = session.getTrackScheduler().getStatus();
    if (status.currentTrack() == null) {
      return InteractionApplicationCommandCallbackSpec.builder()
          .addEmbed(simpleMessageEmbed("Beets Queue", "Queue up some beets!"))
          .build();
    }
    
    StringJoiner joiner = new StringJoiner("\n");
    joiner.add(format(
        "Currently Playing: (%s) %s",
        formatSeek(status.currentTrack().getPosition(), status.currentTrack().getDuration()),
        status.currentTrack().getInfo().title));
    
    int idx = 1;
    for (AudioTrack track : status.queue()) {
      joiner.add(
          format(
              "%d. (%s) %s",
              idx++,
              track.getPosition() == 0
                  ? formatDuration(track.getDuration())
                  : formatSeek(track.getPosition(), track.getDuration()),
              track.getInfo().title));
    }
    return InteractionApplicationCommandCallbackSpec.builder()
        .addEmbed(simpleMessageEmbed("Beets Queue", joiner.toString()))
        .addComponent(actionRow())
        .build();
  }
  
  public static ActionRow actionRow() {
    return ActionRow.of(
        Button.secondary("cmd:stop", ReactionEmoji.unicode("\u23F9"), "Stop"),
        Button.secondary("cmd:skip", ReactionEmoji.unicode("\u23ED"), "Skip"),
        Button.secondary("cmd:punt", ReactionEmoji.unicode("\u21A9"), "Punt"),
        Button.secondary("cmd:promote", ReactionEmoji.unicode("\u2934"), "Promote"));
  }
  
  public static String formatDuration(long durationMillis) {
    Duration duration = Duration.ofMillis(durationMillis);
    return duration.toHours() == 0
        ? format(
            "%02d:%02d",
            duration.toMinutes(), duration.toSecondsPart())
        : format(
            "%02d:%02d:%02d",
            duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
  }

  public static String formatSeek(long seekMillis, long durationMillis) {
    Duration seekDuration = Duration.ofMillis(seekMillis);
    Duration duration = Duration.ofMillis(durationMillis);
    return duration.toHours() == 0
        ? format(
            "%02d:%02d / %02d:%02d",
            seekDuration.toMinutes(), seekDuration.toSecondsPart(),
            duration.toMinutes(), duration.toSecondsPart())
        : format(
            "%02d:%02d:%02d / %02d:%02d:%02d",
            seekDuration.toHours(), seekDuration.toMinutesPart(), seekDuration.toSecondsPart(),
            duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
  }
}
