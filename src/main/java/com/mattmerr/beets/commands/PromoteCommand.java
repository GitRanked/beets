package com.mattmerr.beets.commands;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.commands.CommandDesc.Option;
import com.mattmerr.beets.vc.VCManager;
import com.mattmerr.beets.vc.VCSession;
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
    VCSession session = vcManager.pollSession(event);
    if (session.getTrackScheduler().promote(index)) {
      return event.reply(
          format("<@%s> Promoted!", event.getInteraction().getUser().getId().asString()));
    } else {
      return event.reply(
          format("<@%s> Can't promote index %d!",
                 event.getInteraction().getUser().getId().asString(),
                 index));
    }
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
