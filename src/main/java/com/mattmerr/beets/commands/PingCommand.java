package com.mattmerr.beets.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

@CommandDesc(
    name = "ping",
    description = "Ping Pong Ping Pong",
    options = {}
)
@Singleton
public class PingCommand extends CommandBase {

    @Inject
    public PingCommand() {}
    
    @Override
    public Mono<Void> execute(ChatInputInteractionEvent event) {
        return event.reply("Pong!");
    }
}
