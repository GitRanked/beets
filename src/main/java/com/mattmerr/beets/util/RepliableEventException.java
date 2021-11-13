package com.mattmerr.beets.util;

import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public abstract class RepliableEventException extends RuntimeException {

  protected final Logger log = LoggerFactory.getLogger(this.getClass());

  public abstract Mono<Void> replyToEvent(InteractionCreateEvent event);

  public static class MissingGuildException extends RepliableEventException {

    @Override
    public Mono<Void> replyToEvent(InteractionCreateEvent event) {
      return event.reply("This command must be run within a Guild!");
    }
  }
}
