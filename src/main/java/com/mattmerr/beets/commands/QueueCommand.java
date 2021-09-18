package com.mattmerr.beets.commands;

import static discord4j.core.event.EventDispatcher.log;
import static java.lang.String.format;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.vc.VCManager;
import com.mattmerr.beets.vc.VCSession;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.PartialMember;
import discord4j.rest.util.ApplicationCommandOptionType;
import java.util.StringJoiner;
import reactor.core.publisher.Mono;

@CommandDesc(
    name = "queue",
    description = "queue the beets",
    options = {}
)
@Singleton
public class QueueCommand implements Command {

  private final VCManager vcManager;

  @Inject
  QueueCommand(VCManager vcManager) {
    this.vcManager = vcManager;
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
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
  
  public static String responseForSession(VCSession session) {
    if (session == null) {
      return "Queue up some beets!";
    }
    
    var playingTrack = session.getPlayingTrack();
    var tracks = session.getQueuedTracks();
    if (playingTrack == null) {
      return "Queue up some beets!";
    }
    
    StringJoiner joiner = new StringJoiner("\n");
    joiner.add("Currently Playing: " + playingTrack.getInfo().title);
    int idx = 1;
    for (AudioTrack track : tracks) {
      joiner.add(format("%d. %s", idx++, track.getInfo().title));
    }
    return joiner.toString();
  }
}
