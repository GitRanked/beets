package com.mattmerr.jc.beets;

import org.javacord.api.interaction.SlashCommand;

public class PlayCommandAction extends SlashCommandAction {

    public PlayCommandAction() {
        super(SlashCommand.with("ping", "Checks the functionality of this command"));
    }
}
