package com.mattmerr.beets.commands;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.vc.VCManager;
import discord4j.core.event.domain.interaction.ButtonInteractEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
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
public class StopCommand extends CommandBase implements ButtonCommand {

  private final VCManager vcManager;

  @Inject
  StopCommand(VCManager vcManager) {
    this.vcManager = vcManager;
  }

  public Mono<Void> execute(InteractionCreateEvent event) {
    logCall(event);
    return event.getInteraction().getGuild().flatMap(
        guild ->
            guild.getVoiceConnection().flatMap(
                conn -> executeWithContext(event, guild, conn)))
        .doOnError(e -> log.error("Error processing Skip", e))
        .onErrorResume(e -> event.reply("Error trying to Skip!"));
  }

  @Override
  public Mono<Void> execute(ButtonInteractEvent event) {
    return execute((InteractionCreateEvent) event);
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
    return execute((InteractionCreateEvent) event);
  }
  
  public Mono<Void> executeWithContext(InteractionCreateEvent event, Guild guild, VoiceConnection conn) {
    return conn.disconnect().then(
        guild.getMemberById(event.getInteraction().getUser().getId())
        .flatMap(PartialMember::getVoiceState)
        .flatMap(VoiceState::getChannel)
        .map(vcManager::getSessionOrNull)
        .map(session -> {
          session.destroy();
          return format("<@%s> Stopped!", event.getInteraction().getUser().getId().asString());
        })).flatMap(event::reply);
  }

}
