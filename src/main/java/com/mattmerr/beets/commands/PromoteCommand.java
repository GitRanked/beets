package com.mattmerr.beets.commands;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.commands.CommandDesc.Option;
import com.mattmerr.beets.vc.VCManager;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ButtonInteractEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import java.awt.Button;
import reactor.core.publisher.Mono;

@CommandDesc(
    name = "promote",
    description = "Promote a beet",
    options = {
      @Option(
          name = "index",
          description = "Which beet?",
          type = ApplicationCommandOptionType.INTEGER,
          required = false)
    })
@Singleton
public class PromoteCommand extends CommandBase implements ButtonCommand {

  private final VCManager vcManager;

  @Inject
  PromoteCommand(VCManager vcManager) {
    this.vcManager = vcManager;
  }

  public Mono<Void> execute(InteractionCreateEvent event, Long index) {
    logCall(event);

    return vcManager
        .getChannelForInteraction(event.getInteraction())
        .map(vcManager::getSessionOrNull)
        .flatMap(
            session -> {
              if (session == null || session.getStatus().currentTrack() == null || session.getQueuedTracks().isEmpty()) {
                return event.reply("Nothing to punt!");
              }
              if (session.promote(index)) {
                return event.reply(
                    format("<@%s> Promoted!", event.getInteraction().getUser().getId().asString()));
              } else {
                return event.reply(
                    format("<@%s> Couldn't find that track!", event.getInteraction().getUser().getId().asString()));
              }
            })
        .doOnError(e -> log.error("Error processing Punt", e))
        .onErrorResume(e -> event.reply("Error trying to Punt!"));
  }

  @Override
  public Mono<Void> execute(ButtonInteractEvent event) {
    return execute((InteractionCreateEvent) event, null);
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
    return execute(
        (InteractionCreateEvent) event,
        event
            .getOption("index")
            .get()
            .getValue()
            .map(ApplicationCommandInteractionOptionValue::asLong)
            .orElse(null));
  }
}
