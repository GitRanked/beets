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
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class CachedBeetLoader {

  private record CacheKey(String beet, Boolean isPlaylist) {}

  private static final Logger log =
      LoggerFactory.getLogger(CachedBeetLoader.class);

  private static final String UNABLE_TO_LOAD = "Unable to load beet!";

  private final AudioPlayerManager playerManager;
  private final Cache<CacheKey, List<AudioTrack>> audioTrack =
      CacheBuilder.newBuilder().maximumSize(64).expireAfterWrite(Duration.ofDays(7)).build();

  @Inject
  public CachedBeetLoader(AudioPlayerManager playerManager) {
    this.playerManager = playerManager;
  }

  private synchronized List<AudioTrack> fetchTracks(String beet, boolean loadFullPlaylists) {
    for (int retry = 0; retry < 3; retry++) {
      try {
        var audioLoader = new CompletableAudioLoader(beet, loadFullPlaylists);
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
    return getTracks(beet, false).iterator().next();
  }

  public synchronized List<AudioTrack> getTracks(String beet, boolean loadFullPlaylists) {
    try {
      return audioTrack
              .get(new CacheKey(beet, loadFullPlaylists), () -> this.fetchTracks(beet, loadFullPlaylists))
              .stream()
              .map(AudioTrack::makeClone)
              .collect(Collectors.toList());
    } catch (ExecutionException e) {
      if (e.getCause() instanceof RepliableMessageException ree) {
        throw ree;
      }
      log.error("Error while loading beet", e);
      throw new RepliableMessageException(UNABLE_TO_LOAD);
    }
  }
}
