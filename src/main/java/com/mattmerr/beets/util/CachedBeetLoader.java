package com.mattmerr.beets.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.mattmerr.beets.vc.CompletableAudioLoader;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class CachedBeetLoader {

  private static final Logger log =
      LoggerFactory.getLogger(CachedBeetLoader.class);

  private static final String UNABLE_TO_LOAD = "Unable to load beet!";

  private final AudioPlayerManager playerManager;
  private final Cache<String, AudioTrack> audioTrack =
      CacheBuilder.newBuilder().maximumSize(64).expireAfterWrite(Duration.ofDays(7)).build();

  @Inject
  public CachedBeetLoader(AudioPlayerManager playerManager) {
    this.playerManager = playerManager;
  }

  private synchronized AudioTrack fetchTrack(String beet) {
    for (int retry = 0; retry < 3; retry++) {
      try {
        var audioLoader = new CompletableAudioLoader(beet);
        this.playerManager.loadItem(beet, audioLoader);
        return audioLoader.join();
      } catch (CompletionException ce) {
        try {
          log.error("Caught error! Retrying soon...");
          Thread.sleep(Duration.ofSeconds(1));
        } catch (InterruptedException e) {
          log.error("Interrupted while loading?");
          throw new RepliableMessageException(UNABLE_TO_LOAD);
        }
      }
    }
    throw new RepliableMessageException(UNABLE_TO_LOAD);
  }

  public synchronized AudioTrack getTrack(String beet) {
    try {
      return audioTrack.get(beet, () -> this.fetchTrack(beet)).makeClone();
    } catch (ExecutionException e) {
      if (e.getCause() instanceof RepliableMessageException ree) {
        throw ree;
      }
      log.error("Error while loading beet", e);
      throw new RepliableMessageException(UNABLE_TO_LOAD);
    }
  }
}
