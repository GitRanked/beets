package com.mattmerr.beets.vc;

import com.google.common.collect.ImmutableList;
import com.mattmerr.beets.data.PlayStatus;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import discord4j.common.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {

  private static final Logger log =
      LoggerFactory.getLogger(TrackScheduler.class);

  private final Duration STICK_AROUND_DURATION = Duration.ofMinutes(15);

  private final AudioPlayer player;
  private final LinkedBlockingDeque<AudioTrack> queue;
  private final VCManager manager;
  private final Snowflake guildId;
  private final Snowflake vcId;

  private int retries;

  /**
   * @param player The audio player this scheduler uses
   */
  public TrackScheduler(VCManager manager, Snowflake guildId, Snowflake vcId,
                        AudioPlayer player) {
    this.manager = manager;
    this.guildId = guildId;
    this.vcId = vcId;
    this.player = player;
    this.queue = new LinkedBlockingDeque<>();
    Thread.ofVirtual().name("TrackScheduler").start(this::nextTrack)
        .setUncaughtExceptionHandler(
            (t, e) -> log.error("error with initial track fiber", e));
  }

  public synchronized PlayStatus getStatus() {
    return new PlayStatus(player.getPlayingTrack(), getQueue());
  }

  public synchronized void writeStatus(PlayStatus playStatus) {
    if (playStatus.currentTrack() != null) {
      player.startTrack(playStatus.currentTrack().makeClone(), false);
    }
    queue.clear();
    queue.addAll(playStatus.queue());
  }

  /**
   * Add the next track to queue
   *
   * @param track The track to play or add to queue.
   */
  @CheckReturnValue
  public boolean enqueue(AudioTrack track) {
    return queue.offer(track);
  }

  public ImmutableList<AudioTrack> getQueue() {
    return ImmutableList.copyOf(queue);
  }

  /**
   * Start the next track, stopping the current one if it is playing.
   */
  private void nextTrack() {
    try {
      AudioTrack nextTrack =
          queue.pollFirst(STICK_AROUND_DURATION.getSeconds(), TimeUnit.SECONDS);
      if (nextTrack == null) {
        if (player.getPlayingTrack() != null) {
          log.warn("There's something playing?? Race condition??");
          return;
        }
        log.info("No next item!");
        manager.disconnectFrom(guildId);
      }
      player.startTrack(nextTrack, false);
    } catch (InterruptedException interruptedException) {
      log.error(
          "Error waiting for next track in " + vcId,
          interruptedException);
      manager.disconnectFrom(guildId);
    }
  }

  public synchronized boolean skip() {
    if (player.getPlayingTrack() == null) {
      return false;
    }
    player.stopTrack();
    return true;
  }

  public boolean interject(AudioTrack track) {
    var playing = this.player.getPlayingTrack();
    if (playing != null) {
      var clone = playing.makeClone();
      clone.setPosition(playing.getPosition());
      this.queue.addFirst(clone);
    }
    // Interrupt and play
    return this.player.startTrack(track, false);
  }

  public boolean punt() {
    var playing = this.player.getPlayingTrack();
    if (playing != null && !this.queue.isEmpty()) {
      var clone = playing.makeClone();
      clone.setPosition(playing.getPosition());
      this.queue.addLast(clone);
      nextTrack();
      return true;
    }
    return false;
  }

  public boolean promote(Long index) {
    if (index == null) {
      index = (long) this.queue.size();
    }
    var playing = this.player.getPlayingTrack();
    if (playing == null || index == 0 || this.queue.size() < index) {
      return false;
    }
    var clone = playing.makeClone();
    clone.setPosition(playing.getPosition());
    this.queue.addFirst(clone);
    var q = this.getQueue();
    var toPromote = q.get(index.intValue());
    this.queue.remove(toPromote);
    this.queue.addFirst(toPromote);
    nextTrack();
    return true;
  }

  @Override
  public void onTrackStart(AudioPlayer player, AudioTrack track) {
    super.onTrackStart(player, track);
  }

  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track,
                         AudioTrackEndReason endReason) {
    // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
    log.info("Track ended: " + endReason);
    if (endReason == AudioTrackEndReason.LOAD_FAILED && retries < 3) {
      log.error("LOAD FAILED! Trying again.");
      AudioTrack clone = track.makeClone();
      clone.setPosition(track.getPosition());
      queue.addFirst(clone);
      retries += 1;
    } else {
      retries = 0;
    }
    if (endReason.mayStartNext || (endReason == AudioTrackEndReason.STOPPED)) {
      Thread.ofVirtual()
          .start(this::nextTrack)
          .setUncaughtExceptionHandler(
              (t, e) -> log.error("error with next track", e));
    }
  }
}
