package com.mattmerr.beets.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

public interface ButtonCommand {

  Mono<Void> execute(ButtonInteractionEvent event);

}
