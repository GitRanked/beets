package com.mattmerr.beets.commands;

import com.mattmerr.beets.data.PlayStatus;
import com.mattmerr.beets.vc.VCManager;
import com.mattmerr.beets.vc.VCSession;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

import static com.mattmerr.beets.util.UtilD4J.*;

@CommandDesc(
    name = "yoink",
    description = "Pulls the session to your voice channel",
    options = {}
)
public class YoinkCommand extends CommandBase implements ButtonCommand {

  private final VCManager vcManager;

  YoinkCommand(VCManager vcManager) {
    this.vcManager = vcManager;
  }

  public Mono<Void> execute(DeferrableInteractionEvent event) {
    logCall(event);

    Snowflake guildId = getGuildOrThrow(event);
    Member member = getMemberOrThrow(event);

    Snowflake vcId = vcManager.vcIdForMember(member);
    VCSession curSession = vcManager.peekSessionForGuild(guildId);
    PlayStatus playStatus = null;
    // This does a non-transactional read+write, but I don't care
    if (curSession != null) {
//      playStatus = curSession.getTrackScheduler().getStatus();
//      curSession.disconnect();
      curSession.moveTo(vcId);
    } else {
      vcManager.findOrCreateSession(member).connect();
    }
//    VCSession newSession = vcManager.findOrCreateSession(member);
//    newSession.connect();
//    if (playStatus != null) {
//      newSession.getTrackScheduler().writeStatus(playStatus);
//    }
    return event.reply("Yoink!");
  }

  @Override
  public Mono<Void> execute(ButtonInteractionEvent event) {
    return execute((DeferrableInteractionEvent) event);
  }

  @Override
  public Mono<Void> execute(ChatInputInteractionEvent event) {
    return execute((DeferrableInteractionEvent) event);
  }
}
