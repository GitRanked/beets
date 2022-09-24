package com.mattmerr.beets.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.vc.VCManager;
import com.mattmerr.beets.vc.VCSession;
import discord4j.core.event.domain.interaction.ButtonInteractEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

@CommandDesc(
    name = "skip",
    description = "Skip a beet",
    options = {})
@Singleton
public class SkipCommand extends CommandBase implements ButtonCommand {

  private final VCManager vcManager;

  @Inject
  SkipCommand(VCManager vcManager) {
    this.vcManager = vcManager;
  }
  
  public Mono<Void> execute(InteractionCreateEvent event) {
    logCall(event);
    VCSession session = vcManager.pollSession(event);
    if (!session.getTrackScheduler().skip()) {
      return event.reply("Nothing to skip!");
    }
    String userIdString = event.getInteraction().getUser().getId().asString();
    return event.reply(format("<@%s> Skipped!", userIdString));
  }

  @Override
  public Mono<Void> execute(ButtonInteractEvent event) {
    return execute((InteractionCreateEvent) event);
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
    return execute((InteractionCreateEvent) event);
  }
}
