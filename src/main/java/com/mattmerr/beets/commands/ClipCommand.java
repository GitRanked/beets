package com.mattmerr.beets.commands;

import static com.mattmerr.beets.data.Clip.VALID_CLIP_NAME;
import static com.mattmerr.beets.util.UtilD4J.asRequiredString;
import static com.mattmerr.beets.util.UtilD4J.requireGuildId;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.data.Clip;
import com.mattmerr.beets.data.ClipManager;
import com.mattmerr.beets.util.CachedBeetLoader;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import java.sql.SQLException;
import java.util.Locale;
import reactor.core.publisher.Mono;

@CommandDesc(
    name = "clip",
    description = "Clip it! (use /play to play a saved clip)",
    options = {
      @CommandDesc.Option(
          name = "name",
          description = "What's the beet?",
          type = ApplicationCommandOption.Type.STRING,
          required = true),
      @CommandDesc.Option(
          name = "beet",
          description = "where's the beet?",
          type = ApplicationCommandOption.Type.STRING,
          required = true),
    })
@Singleton
public class ClipCommand extends CommandBase {

  private final ClipManager clipMgr;
  private final CachedBeetLoader beetLoader;

  @Inject
  ClipCommand(ClipManager clipMgr, CachedBeetLoader beetLoader) {
    this.clipMgr = clipMgr;
    this.beetLoader = beetLoader;
  }

  @Override
  public Mono<Void> execute(ChatInputInteractionEvent event) {
    var clipName = asRequiredString(event.getOption("name")).toLowerCase(Locale.ROOT);
    var clipBeet = asRequiredString(event.getOption("beet"));
    var guildId = requireGuildId(event.getInteraction()).asString();

    if (!VALID_CLIP_NAME.matcher(clipName).matches()) {
      return event.reply("Invalid clip name! Clips must be 3-20 letters.");
    }
    AudioTrack audioTrack;
    try {
      audioTrack = beetLoader.getTrack(clipBeet);
      assert audioTrack != null;
    } catch (Exception e) {
      log.error("Failed to load beet", e);
      return event.reply("Could not validate beet!");
    }
    if (upsertClip(guildId, clipName, clipBeet, audioTrack.getInfo().title)) {
      return event.reply(format("Saved clip \"%s\"!", clipName));
    } else {
      return event.reply("Could not save clip :(");
    }
  }

  private boolean upsertClip(String guild, String clipName, String clipBeet, String title) {
    try {
      clipMgr.upsertClip(
          new Clip(guild, clipName, clipBeet, title));
      return true;
    } catch (SQLException sqlException) {
      log.error("Could not save clip", sqlException);
      return false;
    }
  }
}
