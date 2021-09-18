package com.mattmerr.beets.vc;

import com.google.common.collect.ImmutableList;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.VoiceChannelJoinSpec;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;

import static discord4j.core.event.EventDispatcher.log;

public class VCSession {

    private final GatewayDiscordClient client;
    private final VCManager manager;

    VoiceChannel vc;
    AudioPlayer player;
    LavaPlayerAudioProvider provider;
    TrackScheduler trackScheduler;

    private Mono<VoiceConnection> conn = null;

    public VCSession(VCManager manager,
            GatewayDiscordClient client,
            VoiceChannel vc,
            AudioPlayerManager playerManager) {
        this.manager = manager;
        this.client = client;
        this.vc = vc;
        this.player = playerManager.createPlayer();
        this.player.setVolume(100);

        this.provider = new LavaPlayerAudioProvider(this.player);
        this.trackScheduler = new TrackScheduler(this.player);
        this.player.addListener(this.trackScheduler);
    }

    public synchronized Mono<VoiceConnection> connect() {
        if (conn != null) {
            return conn;
        }
        return conn =
                vc.join(VoiceChannelJoinSpec.builder()
                                            .provider(provider)
                                            .build())
                  .doOnNext(voiceConnection -> {
                      log.info(voiceConnection.toString());
                      voiceConnection
                              .onConnectOrDisconnect()
                              .subscribe(
                                      state -> {
                                          log.info(state.name());
                                          if (state == VoiceConnection.State.DISCONNECTED) {
                                              manager.onDisconnect(vc.getId());
                                          }
                                      });
                  }).share();
    }
    
    public void skip() {
      this.trackScheduler.skip();
    }
    
    public AudioTrack getPlayingTrack() {
      return player.getPlayingTrack();
    }
  
    public ImmutableList<AudioTrack> getQueuedTracks() {
      return trackScheduler.getQueue();
    }
}
