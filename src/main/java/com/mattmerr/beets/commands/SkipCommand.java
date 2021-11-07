package com.mattmerr.beets.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.vc.VCManager;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import reactor.core.publisher.Mono;

@CommandDesc(
    name = "skip",
    description = "Skip a beet",
    options = {})
@Singleton
public class SkipCommand extends CommandBase {

  private final VCManager vcManager;

  @Inject
  SkipCommand(VCManager vcManager) {
    this.vcManager = vcManager;
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
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
              return event.reply("Skipped!");
            })
        .doOnError(e -> log.error("Error processing Skip", e))
        .onErrorResume(e -> event.reply("Error trying to Skip!"));
  }
}
