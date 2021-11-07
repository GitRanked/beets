package com.mattmerr.beets.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.vc.VCManager;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.PartialMember;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;

@CommandDesc(
    name = "stop",
    description = "Stops the session and leaves",
    options = {}
)
@Singleton
public class StopCommand extends CommandBase {

  private final VCManager vcManager;

  @Inject
  StopCommand(VCManager vcManager) {
    this.vcManager = vcManager;
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
    logCall(event);
    return event.getInteraction().getGuild().flatMap(
        guild ->
            guild.getVoiceConnection().flatMap(
                conn -> executeWithContext(event, guild, conn)))
        .doOnError(e -> log.error("Error processing Skip", e))
        .onErrorResume(e -> event.reply("Error trying to Skip!"));
  }
  
  public Mono<Void> executeWithContext(SlashCommandEvent event, Guild guild, VoiceConnection conn) {
    return conn.disconnect().then(
        guild.getMemberById(event.getInteraction().getUser().getId())
        .flatMap(PartialMember::getVoiceState)
        .flatMap(VoiceState::getChannel)
        .map(vcManager::getSessionOrNull)
        .map(session -> {
          session.destroy();
          return "Stopped";
        })).flatMap(event::reply);
  }

}
