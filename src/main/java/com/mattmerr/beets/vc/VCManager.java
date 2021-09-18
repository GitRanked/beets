package com.mattmerr.beets.vc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.channel.VoiceChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Mono;
import reactor.netty.FutureMono;

@Singleton
public class VCManager {

  private final ConcurrentHashMap<Snowflake, VCSession> sessionsByVC = new ConcurrentHashMap<>();
  private final GatewayDiscordClient client;

  private final AudioPlayerManager playerManager;

  @Inject
  public VCManager(GatewayDiscordClient client) {
    this.client = client;

    // Creates AudioPlayer instances and translates URLs to AudioTrack instances
    this.playerManager = new DefaultAudioPlayerManager();

    // This is an optimization strategy that Discord4J can utilize.
    // It is not important to understand
    playerManager.getConfiguration()
        .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);

    // Allow playerManager to parse remote sources like YouTube links
    AudioSourceManagers.registerRemoteSources(playerManager);
  }

  public VCSession getActiveSession(VoiceChannel vc) {
    return sessionsByVC.computeIfAbsent(
        vc.getId(),
        key -> new VCSession(this, client, vc, playerManager));
  }

  public VCSession getSessionOrNull(VoiceChannel vc) {
    return sessionsByVC.computeIfAbsent(
        vc.getId(),
        key -> new VCSession(this, client, vc, playerManager));
  }

  public Mono<Void> enqueue(SlashCommandEvent event, VoiceChannel channel, String beet) {
    VCSession session = getActiveSession(channel);

    return session.connect()
        .flatMap(
            conn -> {
              var loader = new CompletableAudioLoader(beet);
              playerManager.loadItemOrdered(channel.getId(), beet, loader);
              return FutureMono.fromFuture(loader);
            })
        .flatMap(
            loader -> {
              if (loader.track != null) {
                session.trackScheduler.enqueue(loader.track);
              }
              return event.reply(loader.message);
            }
        );
  }

  public void onDisconnect(Snowflake vcId) {
    VCSession session = sessionsByVC.remove(vcId);
    if (session != null) {
      session.player.destroy();
    }
  }

}
