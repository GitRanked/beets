package com.mattmerr.beets.vc;

import com.google.common.collect.ImmutableList;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import java.util.concurrent.LinkedBlockingDeque;
import javax.annotation.CheckReturnValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {
  
  private static final Logger log = LoggerFactory.getLogger(TrackScheduler.class);
  
  private final VCSession session;
  private final AudioPlayer player;
  private final LinkedBlockingDeque<AudioTrack> queue;

  /**
   * @param player The audio player this scheduler uses
   */
  public TrackScheduler(VCSession session, AudioPlayer player) {
    this.session = session;
    this.player = player;
    this.queue = new LinkedBlockingDeque<>();
  }

  /**
   * Add the next track to queue or play right away if nothing is in the queue.
   *
   * @param track The track to play or add to queue.
   */
  @CheckReturnValue
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
  private void nextTrack() {
    // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
    // giving null to startTrack, which is a valid argument and will simply stop the player.
    AudioTrack nextTrack = queue.poll();
    if (nextTrack == null) {
      log.info("No next item!");
      session.disconnect();
    }
    player.startTrack(nextTrack, false);
  }
  
  public void skip() {
    player.stopTrack();
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

  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
    // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
    log.info("Track ended: " + endReason);
    if (endReason.mayStartNext || (endReason == AudioTrackEndReason.STOPPED)) {
      nextTrack();
    }
  }
}
