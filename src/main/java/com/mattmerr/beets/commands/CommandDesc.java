package com.mattmerr.beets.commands;

import discord4j.core.object.command.ApplicationCommandOption;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandDesc {
    
    @interface Option {
        String name();
        String description();
        ApplicationCommandOption.Type type();
        boolean required();
    }
    
    String name();
    String description();
    Option[] options();
    
}
