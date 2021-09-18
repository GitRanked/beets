package com.mattmerr.beets.commands;

import discord4j.rest.util.ApplicationCommandOptionType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandDesc {
    
    @interface Option {
        String name();
        String description();
        ApplicationCommandOptionType type();
        boolean required();
    }
    
    String name();
    String description();
    Option[] options();
    
}
