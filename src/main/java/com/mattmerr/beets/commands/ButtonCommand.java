package com.mattmerr.beets.commands;

import discord4j.core.event.domain.interaction.ButtonInteractEvent;
import reactor.core.publisher.Mono;

public interface ButtonCommand {

  Mono<Void> execute(ButtonInteractEvent event);

}
