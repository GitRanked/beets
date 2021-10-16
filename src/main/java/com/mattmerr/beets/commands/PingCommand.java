package com.mattmerr.beets.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.BeetsBot;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@CommandDesc(
    name = "ping",
    description = "Ping Pong Ping Pong",
    options = {}
)
@Singleton
public class PingCommand extends CommandBase {

    @Inject
    PingCommand() {}
    
    @Override
    public Mono<Void> execute(SlashCommandEvent event) {
        return event.reply("Pong!");
    }
}
