package com.mattmerr.beets.commands;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CommandBase implements Command {
  
  protected final Logger log = LoggerFactory.getLogger(this.getClass());
  
  protected final CommandDesc desc = requireNonNull(
      this.getClass().getAnnotation(CommandDesc.class));
  
  protected void logCall(SlashCommandEvent event) {
    log.info(format(
        "[%s]: /%s",
        event.getInteraction().getGuildId().map(Object::toString).orElse("???"),
        desc.name()));
  }

}
