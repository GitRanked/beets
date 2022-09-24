package com.mattmerr.beets.vc;

import com.mattmerr.beets.data.PlayStatus;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.VoiceChannelJoinSpec;
import discord4j.voice.VoiceConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class VCSession {

  private static final Logger log = LoggerFactory.getLogger(VCManager.class);
  private static final Scheduler pool = Schedulers.newParallel("vcsession", 2); 

  private final VCManager manager;

  private VoiceChannel vc;
  private final AudioPlayer player;
  private final LavaPlayerAudioProvider provider;
  private final TrackScheduler trackScheduler;

  private VoiceConnection conn = null;
  private Disposable stateSub;

  public VCSession(
      VCManager manager,
      GatewayDiscordClient client,
      Snowflake vcId,
      AudioPlayerManager playerManager) {
    this.manager = manager;
    this.vc = (VoiceChannel) client.getChannelById(vcId).block();
    this.player = playerManager.createPlayer();
    this.player.setVolume(30);

    this.provider = new LavaPlayerAudioProvider(this.player);
    this.trackScheduler = new TrackScheduler(this, this.player);
    this.player.addListener(this.trackScheduler);

    log.info("Session created for " + vcId);
  }

  public TrackScheduler getTrackScheduler() {
    return trackScheduler;
  }

  public Snowflake getVoiceChannelId() {
    return vc.getId();
  }

  public synchronized void connect() {
    if (conn != null) {
      return;
    }
    VoiceConnection voiceConnection =
        vc.join(VoiceChannelJoinSpec.builder().provider(provider).build())
            .block();
    assert voiceConnection != null;
    log.info("Connected to voice in guild {}", voiceConnection.getGuildId());
    stateSub =
        voiceConnection
            .stateEvents()
            .subscribe(
                state -> {
                  log.info(state.name());
                  if (state == VoiceConnection.State.DISCONNECTED) {
                    manager.onDisconnect(vc.getGuildId());
                  }
                });
  }

  public synchronized void moveTo(Snowflake vc) {
    this.vc = (VoiceChannel) this.vc.getClient().getChannelById(vc).block();
    conn = null;
    if (stateSub != null && !stateSub.isDisposed()) {
      stateSub.dispose();
      stateSub = null;
    }
    connect();
  }

  public synchronized void disconnect() {
    if (conn != null) {
      conn.disconnect().block();
      log.info("Disconnected!");
      conn = null;
    } else {
      log.info("Not connected!");
    }
  }

  public synchronized void destroy() {
    conn = null;
    player.destroy();
    if (stateSub != null && !stateSub.isDisposed()) {
      stateSub.dispose();
      stateSub = null;
    }
  }
}
