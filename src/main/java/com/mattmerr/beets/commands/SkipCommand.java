package com.mattmerr.beets.commands;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.vc.VCManager;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ButtonInteractEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import java.awt.Button;
import reactor.core.publisher.Mono;

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

    return vcManager
        .getChannelForInteraction(event.getInteraction())
        .map(vcManager::getSessionOrNull)
        .flatMap(
            session -> {
              if (session == null || session.getStatus().currentTrack() == null) {
                return event.reply("Nothing to skip!");
              }
              session.skip();
              return event.reply(
                  format("<@%s> Skipped!", event.getInteraction().getUser().getId().asString()));
            })
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
}
