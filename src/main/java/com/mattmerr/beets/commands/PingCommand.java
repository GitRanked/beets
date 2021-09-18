package com.mattmerr.beets.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

@CommandDesc(
    name = "ping",
    description = "Ping Pong Ping Pong",
    options = {}
)
@Singleton
public class PingCommand implements Command {
    
    @Inject
    PingCommand() {}
    
    @Override
    public Mono<Void> execute(SlashCommandEvent event) {
        return event.reply("Pong!");
    }
}
