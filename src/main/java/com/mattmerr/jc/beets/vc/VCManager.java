//package com.mattmerr.jc.beets.vc;
//
//import com.google.inject.Inject;
//import com.google.inject.Singleton;
//import com.mattmerr.beets.util.CachedBeetLoader;
//import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
//import discord4j.common.util.Snowflake;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import reactor.core.publisher.Mono;
//
//import java.util.concurrent.ConcurrentHashMap;
//
//import static reactor.core.publisher.Mono.error;
//
//@Singleton
//public class VCManager {
//
//  private static final Logger log = LoggerFactory.getLogger(VCManager.class);
//
//  private final ConcurrentHashMap<Snowflake, VCSession> sessionsById = new ConcurrentHashMap<>();
////  private final Client client;
//
//  private final AudioPlayerManager playerManager;
//  private final CachedBeetLoader beetLoader;
//
//  @Inject
//  public VCManager(
//      GatewayDiscordClient client, AudioPlayerManager playerManager, CachedBeetLoader beetLoader) {
//    this.client = client;
//    this.playerManager = playerManager;
//    this.beetLoader = beetLoader;
//  }
//
//  public Mono<VoiceChannel> getChannelForInteraction(Interaction interaction) {
//    return interaction
//        .getMember()
//        .map(member -> member.getVoiceState().flatMap(VoiceState::getChannel))
//        .orElseGet(() -> error(new IllegalArgumentException("Event missing interaction member?")));
//  }
//
//  public VCSession getActiveSession(VoiceChannel vc) {
//    return sessionsByVC.computeIfAbsent(
//        vc.getId(), key -> new VCSession(this, client, vc, playerManager));
//  }
//
//  public Mono<VCSession> getActiveSessionForInteraction(Interaction interaction) {
//    return getChannelForInteraction(interaction).map(this::getActiveSession);
//  }
//
//  public VCSession getSessionOrNull(VoiceChannel vc) {
//    return sessionsByVC.computeIfAbsent(
//        vc.getId(), key -> new VCSession(this, client, vc, playerManager));
//  }
//
//  public Mono<VCSession> getSessionOrNullForInteraction(Interaction interaction) {
//    return getChannelForInteraction(interaction).map(this::getSessionOrNull);
//  }
//
//  public Mono<Void> enqueue(SlashCommandEvent event, VoiceChannel channel, String beet) {
//    VCSession session = getActiveSession(channel);
//
//    return session
//        .connect()
//        .flatMap(conn -> beetLoader.getTrack(beet))
//        .flatMap(
//            audioTrack ->
//                session.trackScheduler.enqueue(audioTrack.makeClone())
//                    ? event.reply(
//                        (session.getStatus().queue().isEmpty()
//                                ? "Now playing: "
//                                : "Queued up: ")
//                            + beet)
//                    : event.reply("Play queue is full!"))
//        .doOnError(e -> log.error("Error trying to play", e))
//        .onErrorResume(e -> event.reply("Error trying to play!"));
//  }
//
//  public Mono<Boolean> interject(SlashCommandEvent event, VoiceChannel channel, String beet) {
//    VCSession session = getActiveSession(channel);
//
//    return session
//        .connect()
//        .flatMap(conn -> beetLoader.getTrack(beet))
//        .map(audioTrack -> session.trackScheduler.interject(audioTrack.makeClone()))
//        .doOnError(e -> log.error("Error trying to play", e))
//        //        .onErrorResume(e -> event.reply("Error trying to play!"));
//    ;
//  }
//
//  public void onDisconnect(Snowflake vcId) {
//    log.info("Disconnected from " + vcId);
//    VCSession session = sessionsByVC.remove(vcId);
//    if (session != null) {
//      session.destroy();
//    }
//  }
//}
