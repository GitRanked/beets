package com.mattmerr.beets.vc;

import com.google.common.collect.ImmutableList;
import com.mattmerr.beets.data.PlayStatus;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.VoiceChannelJoinSpec;
import discord4j.voice.VoiceConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class VCSession {

  private static final Logger log = LoggerFactory.getLogger(VCManager.class);
  private static final Scheduler pool = Schedulers.newParallel("vcsession", 2); 

  private final GatewayDiscordClient client;
  private final VCManager manager;

  VoiceChannel vc;
  AudioPlayer player;
  LavaPlayerAudioProvider provider;
  TrackScheduler trackScheduler;

  private Mono<VoiceConnection> conn = null;
  private Disposable stateSub;

  public VCSession(
      VCManager manager,
      GatewayDiscordClient client,
      VoiceChannel vc,
      AudioPlayerManager playerManager) {
    this.manager = manager;
    this.client = client;
    this.vc = vc;
    this.player = playerManager.createPlayer();
    this.player.setVolume(30);

    this.provider = new LavaPlayerAudioProvider(this.player);
    this.trackScheduler = new TrackScheduler(this, this.player);
    this.player.addListener(this.trackScheduler);

    log.info("Session created for " + vc.getId());
  }

  public synchronized Mono<VoiceConnection> connect() {
    if (conn != null) {
      return conn;
    }
    return conn =
        vc.join(VoiceChannelJoinSpec.builder().provider(provider).build())
            .doOnNext(
                voiceConnection -> {
                  log.info(voiceConnection.toString());
                  stateSub =
                      voiceConnection
                          .stateEvents()
                          .subscribe(
                              state -> {
                                log.info(state.name());
                                if (state == VoiceConnection.State.DISCONNECTED) {
                                  manager.onDisconnect(vc.getId());
                                }
                              });
                })
            .subscribeOn(pool)
            .share();
  }

  public synchronized void skip() {
    this.trackScheduler.skip();
  }

  public synchronized PlayStatus getStatus() {
    return new PlayStatus(player.getPlayingTrack(), trackScheduler.getQueue());
  }

  public synchronized ImmutableList<AudioTrack> getQueuedTracks() {
    return trackScheduler.getQueue();
  }

  public synchronized void disconnect() {
    if (conn != null) {
      conn.flatMap(VoiceConnection::disconnect)
          .publishOn(pool)
          .doOnNext(onDisconnect -> {
            log.info("Disconnected!");
          })
          .doOnError(e -> log.error("Error trying to disconnect " + e))
          .subscribe();
      conn = null;
    } else {
      log.info("Not connected!");
    }
  }

  public synchronized void destroy() {
    conn = null;
    player.destroy();
    if (stateSub != null) stateSub.dispose();
  }

  public boolean punt() {
    return this.trackScheduler.punt();
  }

  public boolean promote(Long index) {
    return this.trackScheduler.promote(index);
  }
}
