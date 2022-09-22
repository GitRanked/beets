package com.mattmerr.jc.beets;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.VoiceChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageEditEvent;

public class JCBot {

    public static void main(String[] args) {
        DiscordApi api = new DiscordApiBuilder()
                .setToken("<your super secret token>")
                .login().join();

        var vc = api.getVoiceChannelById(190983260631859200L).flatMap(VoiceChannel::asServerVoiceChannel).get();
        vc.connect();
    }
}
