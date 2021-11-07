package com.mattmerr.beets;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provides;
import com.mattmerr.beets.commands.ClipCommand;
import com.mattmerr.beets.commands.ClipListCommand;
import com.mattmerr.beets.commands.Command;
import com.mattmerr.beets.commands.CommandDesc;
import com.mattmerr.beets.commands.PingCommand;
import com.mattmerr.beets.commands.PlayCommand;
import com.mattmerr.beets.commands.QueueCommand;
import com.mattmerr.beets.commands.SkipCommand;
import com.mattmerr.beets.commands.StopCommand;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandManager.class);

    public static final ImmutableList<Class<? extends Command>> commandClasses = ImmutableList.of(
        PingCommand.class,
        PlayCommand.class,
        QueueCommand.class,
        SkipCommand.class,
        StopCommand.class,
        ClipCommand.class,
        ClipListCommand.class
    );
    
    public static final ImmutableMap<String, Class<? extends Command>> commandsByName;
    
    static {
        ImmutableMap.Builder<String, Class<? extends Command>> mapBuilder = ImmutableMap.builder();
        for (var command : commandClasses) {
            var desc = command.getAnnotation(CommandDesc.class);
            if (desc == null) {
                LOGGER.error("No CommanadDesc on " + command.getName());
                continue;
            }
            mapBuilder.put(desc.name(), command);
        }
        commandsByName = mapBuilder.build();
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
    ImmutableList<ApplicationCommandRequest> commandRequests() {
        ImmutableList.Builder<ApplicationCommandRequest> commandsBuilder = ImmutableList.builder();
        
        for (Class<? extends Command> command : commandsByName.values()) {
            var desc = command.getAnnotation(CommandDesc.class);
            commandsBuilder.add(convertDescription(desc));
        }
        
        return commandsBuilder.build();
    }
}
