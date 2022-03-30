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
    name = "punt",
    description = "Punt a beet",
    options = {})
@Singleton
public class PuntCommand extends CommandBase implements ButtonCommand {

  private final VCManager vcManager;

  @Inject
  PuntCommand(VCManager vcManager) {
    this.vcManager = vcManager;
  }

  public Mono<Void> execute(InteractionCreateEvent event) {
    logCall(event);

    return vcManager
        .getChannelForInteraction(event.getInteraction())
        .map(vcManager::getSessionOrNull)
        .flatMap(
            session -> {
              if (session == null || session.getStatus().currentTrack() == null || session.getQueuedTracks().isEmpty()) {
                return event.reply("Nothing to punt!");
              }
              if (session.punt()) {
                return event.reply(
                    format("<@%s> Punted!", event.getInteraction().getUser().getId().asString()));
              } else {
                return event.reply(
                    format("<@%s> Nothing to punt to!", event.getInteraction().getUser().getId().asString()));
              }
            })
        .doOnError(e -> log.error("Error processing Punt", e))
        .onErrorResume(e -> event.reply("Error trying to Punt!"));
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
