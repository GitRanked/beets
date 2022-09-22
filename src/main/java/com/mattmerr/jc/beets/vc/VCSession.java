//package com.mattmerr.jc.beets.vc;
//
//import com.google.common.collect.ImmutableList;
//import com.mattmerr.beets.data.PlayStatus;
//import com.mattmerr.jc.beets.vc.player.LavaPlayerAudioSource;
//import com.mattmerr.jc.beets.vc.player.TrackScheduler;
//import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
//import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
//import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
//import org.javacord.api.DiscordApi;
//import org.javacord.api.audio.AudioConnection;
//import org.javacord.api.entity.channel.ServerVoiceChannel;
//import org.javacord.api.listener.ObjectAttachableListener;
//import org.javacord.api.listener.audio.AudioConnectionAttachableListener;
//import org.javacord.api.util.event.ListenerManager;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import reactor.core.Disposable;
//import reactor.core.scheduler.Scheduler;
//import reactor.core.scheduler.Schedulers;
//
//import java.util.Collection;
//import java.util.concurrent.CompletionException;
//
//public class VCSession implements AudioConnectionAttachableListener, ObjectAttachableListener {
//
//  private static final Logger log = LoggerFactory.getLogger(VCManager.class);
//  private static final Scheduler pool = Schedulers.newParallel("vcsession", 2);
//
//  private final VCManager manager;
//
//  ServerVoiceChannel vc;
//  AudioPlayer player;
//  LavaPlayerAudioSource provider;
//  TrackScheduler trackScheduler;
//
//  private AudioConnection conn = null;
//  private Disposable stateSub;
//
//  public VCSession(
//            DiscordApi api,
//            VCManager manager,
//            ServerVoiceChannel vc,
//            AudioPlayerManager playerManager) {
//    this.manager = manager;
//    this.vc = vc;
//    this.player = playerManager.createPlayer();
//    this.player.setVolume(30);
//
//    this.provider = new LavaPlayerAudioSource(api, this.player);
//    this.trackScheduler = new TrackScheduler(this, this.player);
//    this.player.addListener(this.trackScheduler);
//
//    log.info("Session created for " + vc.getId());
//  }
//
//  public synchronized AudioConnection connect() {
//    if (conn != null) {
//      return conn;
//    }
//    AudioConnection conn = vc.connect().join();
//    conn.setAudioSource(this.provider);
//    conn.addAudioConnectionAttachableListener(this);
////    conn.
//    return null;
//  }
//
//  public synchronized void skip() {
//    this.trackScheduler.skip();
//  }
//
//  public synchronized PlayStatus getStatus() {
//    return new PlayStatus(player.getPlayingTrack(), trackScheduler.getQueue());
//  }
//
//  public synchronized ImmutableList<AudioTrack> getQueuedTracks() {
//    return trackScheduler.getQueue();
//  }
//
//  public synchronized void disconnect() {
//    if (conn == null) {
//      log.info("Not connected!");
//      return;
//    }
//    try {
//      conn.close().join();
//    } catch (CompletionException e) {
//      log.error("Error trying to disconnect from AudioConnection", e.getCause());
//      return;
//    }
//    conn = null;
//    log.info("Disconnected!");
//    player.destroy();
//    player = null;
//  }
//
//  public synchronized void destroy() {
//    conn = null;
//    player.destroy();
//    if (stateSub != null) stateSub.dispose();
//  }
//}
