package com.mattmerr.beets.util;

import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import reactor.core.publisher.Mono;

public class RepliableMessageException extends RuntimeException {

  private final String msg;

  public RepliableMessageException(String msg) {
    this.msg = msg;
  }

  public String getReplyMessage() {
    return msg;
  }

  public final Mono<Void> replyToEvent(InteractionCreateEvent event) {
    return event.reply(getReplyMessage());
  }
}
