package com.mattmerr.beets.commands;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provides;
import com.mattmerr.beets.data.ClipManager;
import com.mattmerr.beets.util.CachedBeetLoader;
import com.mattmerr.beets.vc.VCManager;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandManager.class);

    public final ImmutableList<Command> handlers;
    public final ImmutableMap<String, Command> commandsByName;
    public final ImmutableMap<String, ButtonCommand> buttonsByName;

    public CommandManager(VCManager vcManager,
                   ClipManager clipManager,
                   CachedBeetLoader beetLoader) {
        handlers = ImmutableList.of(
            new PingCommand(),
            new PlayCommand(vcManager, clipManager),
            new QueueCommand(vcManager),
            new SkipCommand(vcManager),
            new StopCommand(vcManager),
            new ClipCommand(clipManager, beetLoader),
            new ClipListCommand(clipManager),
            new PuntCommand(vcManager),
            new PromoteCommand(vcManager));

        ImmutableMap.Builder<String, Command> cmdMapBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<String, ButtonCommand> btnMapBuilder = ImmutableMap.builder();
        for (var handler : handlers) {
            var desc = handler.getClass().getAnnotation(CommandDesc.class);
            if (desc == null) {
                LOGGER.error("No CommandDesc on " + handler.getClass().getName());
                continue;
            }
            cmdMapBuilder.put(desc.name(), handler);

            if (handler instanceof ButtonCommand) {
                btnMapBuilder.put("cmd:" + desc.name(),
                                  (ButtonCommand) handler);
            }
        }
        commandsByName = cmdMapBuilder.build();
        buttonsByName = btnMapBuilder.build();
    }
    
    private static ApplicationCommandRequest convertDescription(CommandDesc desc) {
        var requestBuilder = ApplicationCommandRequest.builder()
            .name(desc.name())
            .description(desc.description());
        
        for (CommandDesc.Option option : desc.options()) {
            requestBuilder.addOption(convertOption(option));
        }
        return requestBuilder.build();
    }
    
    private static ApplicationCommandOptionData convertOption(CommandDesc.Option option) {
        return ApplicationCommandOptionData.builder()
            .name(option.name())
            .description(option.description())
            .type(option.type().getValue())
            .required(option.required())
            .build();
    }

    @Provides
    public ImmutableList<ApplicationCommandRequest> commandRequests() {
        ImmutableList.Builder<ApplicationCommandRequest> commandsBuilder = ImmutableList.builder();
        for (var handler : handlers) {
            var desc = handler.getClass().getAnnotation(CommandDesc.class);
            if (desc == null) {
                LOGGER.error("No CommandDesc on " + handler.getClass().getName());
                continue;
            }
            commandsBuilder.add(convertDescription(desc));
        }
        return commandsBuilder.build();
    }
}
