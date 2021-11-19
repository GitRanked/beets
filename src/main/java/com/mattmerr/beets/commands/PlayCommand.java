package com.mattmerr.beets.commands;

import static com.mattmerr.beets.data.Clip.VALID_CLIP_NAME;
import static com.mattmerr.beets.util.UtilD4J.asRequiredString;
import static com.mattmerr.beets.util.UtilD4J.requireGuildId;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.data.ClipManager;
import com.mattmerr.beets.vc.VCManager;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.rest.util.ApplicationCommandOptionType;
import java.sql.SQLException;
import java.util.Locale;
import reactor.core.publisher.Mono;

@CommandDesc(
    name = "play",
    description = "Pick a beet",
    options = {
        @CommandDesc.Option(
            name = "beet",
            description = "where's the beet?",
            type = ApplicationCommandOptionType.STRING,
            required = true),
    }
)
@Singleton
public class PlayCommand extends CommandBase {

  private final VCManager vcManager;
  private final ClipManager clipMgr;

  @Inject
  PlayCommand(VCManager vcManager, ClipManager clipMgr) {
    this.vcManager = vcManager;
    this.clipMgr = clipMgr;
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
    logCall(event);
    String beet = asRequiredString(event.getOption("beet"));
    String guildId = requireGuildId(event.getInteraction()).asString();

    if (VALID_CLIP_NAME.matcher(beet.toLowerCase(Locale.ROOT)).matches()) {
      try {
        var clipOp = clipMgr.selectClip(guildId, beet.toLowerCase(Locale.ROOT));
        if (clipOp.isPresent()) {
          beet = clipOp.get().beet();
        }
      } catch (SQLException e) {
        log.debug("error looking up beet", e);
      }
    }

    final String beetCapture = beet;
    return vcManager
        .getChannelForInteraction(event.getInteraction())
        .flatMap(vc -> vcManager.enqueue(event, vc, beetCapture))
        .doOnError(e -> log.error("Error processing Play", e))
        .onErrorResume(e -> event.reply("Error trying to Play!"));
  }

}
