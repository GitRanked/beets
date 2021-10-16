package com.mattmerr.beets.vc;

import com.google.common.collect.ImmutableList;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {
  
  private static final Logger log = LoggerFactory.getLogger(TrackScheduler.class);
  
  private final VCSession session;
  private final AudioPlayer player;
  private final ArrayBlockingQueue<AudioTrack> queue;

  /**
   * @param player The audio player this scheduler uses
   */
  public TrackScheduler(VCSession session, AudioPlayer player) {
    this.session = session;
    this.player = player;
    this.queue = new ArrayBlockingQueue<>(16);
  }

  /**
   * Add the next track to queue or play right away if nothing is in the queue.
   *
   * @param track The track to play or add to queue.
   */
  public boolean enqueue(AudioTrack track) {
    // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
    // something is playing, it returns false and does nothing. In that case the player was already playing so this
    // track goes to the queue instead.
    if (!player.startTrack(track, true)) {
      return queue.offer(track);
    }
    return true;
  }
  
  public ImmutableList<AudioTrack> getQueue() {
    return ImmutableList.copyOf(queue);
  }

  /**
   * Start the next track, stopping the current one if it is playing.
   */
  public void nextTrack() {
    // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
    // giving null to startTrack, which is a valid argument and will simply stop the player.
    try {
      AudioTrack nextTrack = queue.poll(5, TimeUnit.SECONDS);
      if (nextTrack == null) {
        log.info("Time out waiting for new item!");
        session.disconnect();
        return;
      }
      player.startTrack(nextTrack, false);
    } catch (InterruptedException interruptedException) {
      log.error("Interrupted waiting for next track", interruptedException);
    }
  }
  
  public void skip() {
    player.stopTrack();
  }

  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
    // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
    log.info("Track ended: " + endReason);
    if (endReason.mayStartNext || (endReason == AudioTrackEndReason.STOPPED)) {
      nextTrack();
    }
  }
}
