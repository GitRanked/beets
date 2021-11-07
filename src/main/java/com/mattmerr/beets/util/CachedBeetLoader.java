package com.mattmerr.beets.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.mattmerr.beets.vc.CompletableAudioLoader;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class CachedBeetLoader {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final AudioPlayerManager playerManager;

  private Cache<String, Mono<AudioTrack>> audioTrack =
      CacheBuilder.newBuilder().maximumSize(64).expireAfterWrite(Duration.ofDays(7)).build();

  @Inject
  public CachedBeetLoader(AudioPlayerManager playerManager) {
    this.playerManager = playerManager;
  }

  public Mono<AudioTrack> fetchTrack(String beet) {
    return Mono.just(new CompletableAudioLoader(beet))
        .flatMap(
            audioLoader -> {
              this.playerManager.loadItem(beet, audioLoader);
              return Mono.fromFuture(audioLoader);
            })
        .retryWhen(Retry.fixedDelay(3, Duration.of(1, ChronoUnit.SECONDS)))
        .cache();
  }

  public Mono<AudioTrack> getTrack(String beet) {
    try {
      return audioTrack.get(beet, () -> this.fetchTrack(beet));
    } catch (ExecutionException e) {
      log.error("Error while loading beet", e);
      throw new RuntimeException(e);
    }
  }
}
