package com.mattmerr.beets.vc;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

public class VoiceModule extends AbstractModule {

  @Provides
  @Singleton
  public AudioPlayerManager providePlayerManager() {
    // Creates AudioPlayer instances and translates URLs to AudioTrack instances
    AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

    // This is an optimization strategy that Discord4J can utilize.
    // It is not important to understand
    playerManager.getConfiguration()
        .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);

    // Allow playerManager to parse remote sources like YouTube links
    AudioSourceManagers.registerRemoteSources(playerManager);
    
    return playerManager;
  }
}
