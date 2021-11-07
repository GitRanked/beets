package com.mattmerr.beets.commands;

import static com.mattmerr.beets.util.UtilD4J.asRequiredString;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.vc.VCManager;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

@CommandDesc(
    name = "play",
    description = "Pick a beet",
    options = {
        @CommandDesc.Option(
            name = "beet",
            description = "where's the beet?",
            type = ApplicationCommandOptionType.STRING,
            required = true),
    }
)
@Singleton
public class PlayCommand extends CommandBase {

  private final VCManager vcManager;

  @Inject
  PlayCommand(VCManager vcManager) {
    this.vcManager = vcManager;
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
    logCall(event);
    String beet = asRequiredString(event.getOption("beet"));
    
    return vcManager.getChannelForInteraction(event.getInteraction())
        .flatMap(vc -> vcManager.enqueue(event, vc, beet))
        .doOnError(e -> log.error("Error processing Play", e))
        .onErrorResume(e -> event.reply("Error trying to Play!"));
  }

}
