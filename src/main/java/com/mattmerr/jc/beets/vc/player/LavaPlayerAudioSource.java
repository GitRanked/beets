package com.mattmerr.jc.beets.vc.player;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.audio.AudioSourceBase;

public final class LavaPlayerAudioSource extends AudioSourceBase {

    private final AudioPlayer audioPlayer;
    private AudioFrame lastFrame;

    public LavaPlayerAudioSource(DiscordApi api, final AudioPlayer player) {
        super(api);
        this.audioPlayer = player;
        this.audioPlayer.setFrameBufferDuration(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize());
    }

    @Override
    public byte[] getNextFrame() {
        if (lastFrame == null) {
            return null;
        }
        return applyTransformers(lastFrame.getData());
    }

    @Override
    public boolean hasFinished() {
        return false;
    }

    @Override
    public boolean hasNextFrame() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public AudioSource copy() {
        return new LavaPlayerAudioSource(getApi(), audioPlayer);
    }
}