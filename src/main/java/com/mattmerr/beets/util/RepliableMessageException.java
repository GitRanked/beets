package com.mattmerr.beets.util;

import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public abstract class RepliableEventException extends RuntimeException {

  protected final Logger log = LoggerFactory.getLogger(this.getClass());

  public abstract Mono<Void> replyToEvent(InteractionCreateEvent event);

  public static class SimpleMessageException extends RepliableEventException {
    private final String msg;

    public SimpleMessageException(String msg) {
      this.msg = msg;
    }

    @Override
    public Mono<Void> replyToEvent(InteractionCreateEvent event) {
      return event.reply(msg);
    }
  }

  public static class MissingGuildException extends SimpleMessageException {
    public MissingGuildException() {
      super("This command must be run within a Guild!");
    }
  }

  public static class NotInVoiceChatException extends SimpleMessageException {
    public NotInVoiceChatException() {
      super("You must join a Voice Channel!");
    }
  }

  public static class NoCurrentSessionException extends SimpleMessageException {
    public NoCurrentSessionException() {
      super("There is not currently a session!");
    }
  }
}
