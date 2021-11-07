package com.mattmerr.beets.commands;

import static com.mattmerr.beets.util.UtilD4J.asRequiredString;
import static com.mattmerr.beets.util.UtilD4J.asString;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.commands.CommandDesc.Option;
import com.mattmerr.beets.data.Clip;
import com.mattmerr.beets.data.ClipManager;
import com.mattmerr.beets.util.CachedBeetLoader;
import com.mattmerr.beets.vc.CompletableAudioLoader;
import com.mattmerr.beets.vc.VCManager;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.rest.util.ApplicationCommandOptionType;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@CommandDesc(
    name = "clip",
    description = "Clip it!",
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
          required = false),
    })
@Singleton
public class ClipCommand extends CommandBase {

  private static final Pattern VALID_CLIP_NAME = Pattern.compile("[a-z]{3,20}");

  private final ClipManager clipMgr;
  private final VCManager vcMgr;
  private final CachedBeetLoader beetLoader;

  @Inject
  ClipCommand(ClipManager clipMgr, VCManager vcMgr, CachedBeetLoader beetLoader) {
    this.clipMgr = clipMgr;
    this.vcMgr = vcMgr;
    this.beetLoader = beetLoader;
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
    var clipName = asRequiredString(event.getOption("name")).toLowerCase(Locale.ROOT);
    var clipPayloadOption = asString(event.getOption("beet"));

    if (!VALID_CLIP_NAME.matcher(clipName.toLowerCase(Locale.ROOT)).matches()) {
      return event.reply("Invalid clip name! Clips must be 3-20 letters.");
    }

    var guildMono = event.getInteraction().getGuild();
    return guildMono.flatMap(
        guild ->
            clipPayloadOption
                .map(beet -> upsertClip(event, guild.getId().asString(), clipName, beet))
                .orElseGet(() -> playClip(event, guild.getId().asString(), clipName)));
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

  private Mono<Void> playClip(SlashCommandEvent event, String guild, String clipName) {
    return Mono.<Optional<Clip>>fromSupplier(
            () -> {
              try {
                return clipMgr.selectClip(guild, clipName);
              } catch (SQLException sqlException) {
                log.error("Could not load clip", sqlException);
                return Optional.empty();
              }
            })
        .subscribeOn(Schedulers.newSingle("clip-play"))
        .flatMap(
            beetOp -> {
              if (beetOp.isEmpty()) {
                return event.reply("Could not load clip with that name!");
              }
              return vcMgr
                  .getChannelForInteraction(event.getInteraction())
                  .flatMap(vc -> vcMgr.enqueue(event, vc, beetOp.get().beet()));
            });
  }
}
