package com.mattmerr.beets;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.mattmerr.beets.commands.Command;
import com.mattmerr.beets.vc.VCManager;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.lifecycle.DisconnectEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.RestClient;
import discord4j.rest.util.Color;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class BeetsBot {

    public static final Color COLOR = Color.of(0xFF4444);
    private static final Logger log = LoggerFactory.getLogger(BeetsBot.class);

    private static final Map<String, Command> commands = new HashMap<>();

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                super.configure();
                final GatewayDiscordClient client = DiscordClientBuilder.create(args[0]).build().login().block();
                bind(GatewayDiscordClient.class).toInstance(client);
                bind(RestClient.class).toInstance(client.getRestClient());

                CommandManager commandManager = new CommandManager();
                bind(CommandManager.class).toInstance(commandManager);
            }
        });
        injector.getInstance(BeetsBot.class);
        
        var client = injector.getInstance(GatewayDiscordClient.class);
        var restClient = injector.getInstance(RestClient.class);
        var commandManager = injector.getInstance(CommandManager.class);
        long applicationId = restClient.getApplicationId().block();
        
        log.info("weee");

        client.getGuilds().map(guild -> {
            restClient.getApplicationService()
                .bulkOverwriteGuildApplicationCommand(
                    applicationId,
                    guild.getId().asLong(),
                    commandManager.commandRequests())
                .doOnError(e -> log.warn(
                    "Unable to create guild command",
                    e))
                .onErrorResume(e -> Mono.empty())
                .blockLast();
            return guild.getId();
        }).blockLast();

        client.on(new ReactiveEventAdapter() {

            @Nonnull
            @Override
            public Publisher<?> onSlashCommand(@Nonnull SlashCommandEvent event) {
                log.info("Received event!");
                try {
                    var cmdClass = CommandManager.commandsByName.get(event.getCommandName());
                    return injector.getInstance(cmdClass)
                        .execute(event)
                        .doOnError(e -> log.warn(
                            "Unable to create guild command",
                            e))
                        .onErrorResume(e -> Mono.empty());
                } catch (Exception err) {
                    err.printStackTrace();
                    return Mono.empty();
                }
            }
        }).doOnError(e -> log.warn(
            "Unable to create guild command",
            e)).blockLast();

        client.onDisconnect().block();
    }
    
    @Inject
    BeetsBot() {
        
    }

}
