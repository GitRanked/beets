package com.mattmerr.beets.commands;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.data.ClipManager;
import com.mattmerr.beets.vc.VCManager;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Guild;
import discord4j.rest.util.ApplicationCommandOptionType;
import java.sql.SQLException;
import java.util.Optional;
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

  private final ClipManager clipMgr;
  private final VCManager vcMgr;

  @Inject
  ClipCommand(ClipManager clipMgr, VCManager vcMgr) {
    this.clipMgr = clipMgr;
    this.vcMgr = vcMgr;
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
    var guildMono = event.getInteraction().getGuild();
    var clipName = event.getOption("name").get().getValue().get().asString();
    var clipPayloadOption =
        event.getOption("beet").flatMap(ApplicationCommandInteractionOption::getValue);
    if (clipPayloadOption.isPresent()) {
      return guildMono.flatMap(
          guild ->
              upsertClip(
                  event, guild.getId().asString(), clipName, clipPayloadOption.get().asString()));
    } else {
      return guildMono.flatMap(
          guild ->
              playClip(
                  event, guild.getId().asString(), clipName));
    }
  }

  private Mono<Void> upsertClip(
      SlashCommandEvent event, String guild, String clipName, String clipBeet) {
    return Mono.defer(
            () -> {
              try {
                clipMgr.upsertClip(guild, clipName, clipBeet);
                return event.reply(format("Saved clip \"%s\"!", clipName));
              } catch (SQLException sqlException) {
                log.error("Could not save clip", sqlException);
                return event.reply("Could not save clip :(");
              }
            })
        .publishOn(Schedulers.newSingle("clip-upsert"));
  }

  private Mono<Void> playClip(SlashCommandEvent event, String guild, String clipName) {
    return Mono.defer(
        () -> {
          try {
            return clipMgr
                .selectClip(guild, clipName)
                .map(s -> event.reply(format("This should play \"%s\"", s)))
                .orElseGet(() -> event.reply("Clip not found :("));
          } catch (SQLException sqlException) {
            log.error("Could not save clip", sqlException);
            return event.reply("Could not save clip :(");
          }
        })
        .publishOn(Schedulers.newSingle("clip-play"));
  }
}
