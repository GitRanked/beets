package com.mattmerr.beets.vc;

import com.mattmerr.beets.util.CachedBeetLoader;
import com.mattmerr.beets.util.RepliableEventException;
import com.mattmerr.beets.util.RepliableEventException.MissingGuildException;
import com.mattmerr.beets.util.RepliableEventException.NoCurrentSessionException;
import com.mattmerr.beets.util.RepliableEventException.SimpleMessageException;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

public class VCManager {

  private static final Logger log = LoggerFactory.getLogger(VCManager.class);

  private final ConcurrentHashMap<Snowflake, VCSession> sessionsByGuild =
      new ConcurrentHashMap<>();
  private final GatewayDiscordClient client;

  private final AudioPlayerManager playerManager;
  private final CachedBeetLoader beetLoader;

  public VCManager(
      GatewayDiscordClient client, AudioPlayerManager playerManager,
      CachedBeetLoader beetLoader) {
    this.client = client;
    this.playerManager = playerManager;
    this.beetLoader = beetLoader;
  }

  public VCSession findOrCreateSession(InteractionCreateEvent event) {
    var member = event.getInteraction().getMember()
        .orElseThrow(MissingGuildException::new);
    return findOrCreateSession(member);
  }

  public VCSession findOrCreateSession(Member member) {
    Snowflake guildId = member.getGuildId();
    Snowflake vcId = member.getVoiceState()
        .map(VoiceState::getChannelId)
        .block()
        .orElseThrow(RepliableEventException.NotInVoiceChatException::new);
    return findOrCreateSession(guildId, vcId);
  }

  public VCSession findOrCreateSession(@Nonnull Snowflake guildId,
                                       @Nonnull Snowflake vcId) {
    checkNotNull(guildId);
    checkNotNull(vcId);
    return sessionsByGuild.compute(guildId, (g, sess) -> {
      if (sess == null) {
        return new VCSession(this, client, vcId, playerManager);
      }
      if (!sess.getVoiceChannelId().equals(vcId)) {
        throw new SimpleMessageException("Beets is already playing!");
      }
      return sess;
    });
  }

  public VCSession peekSession(InteractionCreateEvent event) {
    return peekSessionForGuild(event.getInteraction()
                                   .getGuildId()
                                   .orElseThrow(MissingGuildException::new));
  }

  public VCSession peekSession(Member member) {
    return peekSessionForGuild(member.getGuildId());
  }

  public VCSession peekSessionForGuild(@Nonnull Snowflake guildId) {
    checkNotNull(guildId);
    return sessionsByGuild.get(guildId);
  }

  public VCSession pollSession(InteractionCreateEvent event) {
    return pollSessionForGuild(event.getInteraction()
                                   .getGuildId()
                                   .orElseThrow(MissingGuildException::new));
  }

  public VCSession pollSession(Member member) {
    return pollSessionForGuild(member.getGuildId());
  }

  public VCSession pollSessionForGuild(@Nonnull Snowflake guildId) {
    checkNotNull(guildId);
    VCSession session = sessionsByGuild.get(guildId);
    if (session == null) {
      throw new NoCurrentSessionException();
    }
    return session;
  }

//  public Mono<Void> enqueue(SlashCommandEvent event, VoiceChannel channel,
//                            String beet) {
//    VCSession session = findOrCreateSession(channel);
//    return session
//        .connect()
//        .flatMap(conn -> beetLoader.getTrack(beet))
//        .flatMap(
//            audioTrack ->
//                session.trackScheduler.enqueue(audioTrack.makeClone())
//                    ? event.reply(
//                    (session.getStatus().queue().isEmpty()
//                         ? "Now playing: "
//                         : "Queued up: ")
//                        + beet)
//                    : event.reply("Play queue is full!"))
//        .doOnError(e -> log.error("Error trying to play", e))
//        .onErrorResume(e -> event.reply("Error trying to play!"));
//  }
//
//  public Mono<Boolean> interject(SlashCommandEvent event, VoiceChannel channel,
//                                 String beet) {
//    VCSession session = getActiveSession(channel);
//
//    return session
//        .connect()
//        .flatMap(conn -> beetLoader.getTrack(beet))
//        .map(audioTrack -> session.trackScheduler.interject(
//            audioTrack.makeClone()))
//        .doOnError(e -> log.error("Error trying to play", e))
//        //        .onErrorResume(e -> event.reply("Error trying to play!"));
//        ;
//  }

  public void onDisconnect(Snowflake guildId) {
    log.info("Disconnected from " + guildId);
    VCSession session = sessionsByGuild.remove(guildId);
    if (session != null) {
      session.destroy();
    }
  }
}
