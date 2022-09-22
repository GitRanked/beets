package com.mattmerr.jc.beets;

import org.javacord.api.interaction.SlashCommandBuilder;

public abstract class SlashCommandAction {
    private final SlashCommandBuilder cmdBuilder;

    public SlashCommandAction(SlashCommandBuilder cmdBuilder) {
        this.cmdBuilder = cmdBuilder;
    }


}
