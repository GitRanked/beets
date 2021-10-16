package com.mattmerr.beets.commands;

import static java.lang.String.format;
import static java.lang.String.join;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.BeetsBot;
import com.mattmerr.beets.vc.VCManager;
import com.mattmerr.beets.vc.VCSession;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.component.MessageComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.PartialMember;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Mono;

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
    return event.getInteraction().getGuild().flatMap(
        guild -> guild.getMemberById(event.getInteraction().getUser().getId())
            .flatMap(PartialMember::getVoiceState)
            .flatMap(VoiceState::getChannel)
            .map(vc -> vcManager.getSessionOrNull(vc))
            .map(QueueCommand::responseForSession)
            .flatMap(event::reply))
        .doOnError(e -> log.error("Error processing Play", e))
        .onErrorResume(e -> event.reply("Error trying to Play!"));
  }
  
  public static InteractionApplicationCommandCallbackSpec responseForSession(VCSession session) {
    if (session == null) {
      return InteractionApplicationCommandCallbackSpec.builder()
          .addEmbed(EmbedCreateSpec.builder()
              .color(BeetsBot.COLOR)
              .title("Beets Queue")
              .description("Queue up some beets!")
              .build())
          .build();
    }
    
    var status = session.getStatus();
    if (status.currentTrack() == null) {
      return InteractionApplicationCommandCallbackSpec.builder()
          .addEmbed(EmbedCreateSpec.builder()
              .color(BeetsBot.COLOR)
              .title("Beets Queue")
              .description("Queue up some beets!")
              .timestamp(Instant.now())
              .build())
          .build();
    }
    
    StringJoiner joiner = new StringJoiner("\n");
    joiner.add("Currently Playing: " + status.currentTrack().getInfo().title);
    int idx = 1;
    for (AudioTrack track : status.queue()) {
      joiner.add(format("%d. %s", idx++, track.getInfo().title));
    }
    return InteractionApplicationCommandCallbackSpec.builder()
        .addEmbed(EmbedCreateSpec.builder()
            .color(BeetsBot.COLOR)
            .title("Beets Queue")
            .description(joiner.toString())
            .timestamp(Instant.now())
            .build())
        .build();
  }
  
  public static String formatDuration(long durationMillis) {
    long hours = durationMillis / 1000 / 60 / 60;
    long minutes = durationMillis - 
  }
}
