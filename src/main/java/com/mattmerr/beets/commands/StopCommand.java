package com.mattmerr.beets.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.vc.VCManager;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.entity.Guild;
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

  public Mono<Void> execute(DeferrableInteractionEvent event) {
    logCall(event);
    Guild guild = event.getInteraction().getGuild().block();
    guild.getVoiceConnection().block().disconnect().block();
    return event.reply("Stopped");
  }

  @Override
  public Mono<Void> execute(ButtonInteractionEvent event) {
    return execute((DeferrableInteractionEvent) event);
  }

  @Override
  public Mono<Void> execute(ChatInputInteractionEvent event) {
    return execute((DeferrableInteractionEvent) event);
  }
}
