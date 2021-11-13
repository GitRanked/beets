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
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.rest.util.ApplicationCommandOptionType;
import java.sql.SQLException;
import java.util.Locale;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@CommandDesc(
    name = "clip",
    description = "Clip it! (use /play to play a saved clip)",
    options = {
      @CommandDesc.Option(
          name = "name",
          description = "What's the beet?",
          type = ApplicationCommandOptionType.STRING,
          required = true),
      @CommandDesc.Option(
          name = "beet",
          description = "where's the beet?",
          type = ApplicationCommandOptionType.STRING,
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
  public Mono<Void> execute(SlashCommandEvent event) {
    var clipName = asRequiredString(event.getOption("name")).toLowerCase(Locale.ROOT);
    var clipBeet = asRequiredString(event.getOption("beet"));
    var guildId = requireGuildId(event.getInteraction()).asString();

    if (!VALID_CLIP_NAME.matcher(clipName).matches()) {
      return event.reply("Invalid clip name! Clips must be 3-20 letters.");
    }

    return upsertClip(event, guildId, clipName, clipBeet);
  }

  private Mono<Void> upsertClip(
      SlashCommandEvent event, String guild, String clipName, String clipBeet) {

    return beetLoader
        .getTrack(clipBeet)
        .flatMap(
            audioTrack ->
                Mono.defer(
                        () -> {
                          try {
                            clipMgr.upsertClip(
                                new Clip(guild, clipName, clipBeet, audioTrack.getInfo().title));
                            return event.reply(format("Saved clip \"%s\"!", clipName));
                          } catch (SQLException sqlException) {
                            log.error("Could not save clip", sqlException);
                            return event.reply("Could not save clip :(");
                          }
                        })
                    .publishOn(Schedulers.newSingle("clip-upsert"))
                    .doOnError(e -> log.error("Error loading clip", e)))
        .doOnError(e -> log.error("Error replying with load failure", e))
        .onErrorResume(err -> event.reply("Beet does not load! :("));
  }
}
