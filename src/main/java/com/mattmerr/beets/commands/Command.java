package com.mattmerr.beets.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

public interface Command {
    // Since we are expecting to do reactive things in this method, like    
    // send a message, then this method will also return a reactive type.    
    Mono<Void> execute(ChatInputInteractionEvent event);
}