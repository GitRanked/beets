package com.mattmerr.beets.commands;

import static com.mattmerr.beets.util.UtilD4J.asLong;
import static com.mattmerr.beets.util.UtilD4J.simpleMessageEmbed;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.BeetsBot;
import com.mattmerr.beets.commands.CommandDesc.Option;
import com.mattmerr.beets.data.Clip;
import com.mattmerr.beets.data.ClipManager;
import com.mattmerr.beets.util.RepliableEventException;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.ApplicationCommandOptionType;
import java.sql.SQLException;
import reactor.core.publisher.Mono;

@CommandDesc(
    name = "clips",
    description = "Enumerate clips",
    options = {
      @Option(
          name = "page",
          description = "Page number, for seeing more clips.",
          type = ApplicationCommandOptionType.INTEGER,
          required = false)
    })
@Singleton
public class ClipListCommand extends CommandBase {

  private final ClipManager clipMgr;

  @Inject
  ClipListCommand(ClipManager clipMgr) {
    this.clipMgr = clipMgr;
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
    long page = asLong(event.getOption("page")).orElse(0L);
    var guildId = event.getInteraction().getGuildId().orElseThrow(
        RepliableEventException.MissingGuildException::new);
    EmbedCreateSpec embed;
    try {
      var clips = clipMgr.enumerateClips(guildId.asString(), page);
      embed = clips.isEmpty()
          ? buildResponse("No clips found!")
          : buildResponse(clips);
    } catch (SQLException sqlException) {
      log.error("Error enumerating clips", sqlException);
      embed = buildResponse("Error! Could not retrieve any clips.");
    }
    return event.reply(
      InteractionApplicationCommandCallbackSpec.builder()
          .allowedMentions(AllowedMentions.suppressAll())
          .addEmbed(embed)
          .build());
  }

  private EmbedCreateSpec buildResponse(String message) {
    return EmbedCreateSpec.builder()
        .title("Beets Clips")
        .color(BeetsBot.COLOR)
        .description(message)
        .build();
  }

  private EmbedCreateSpec buildResponse(Iterable<Clip> clips) {
    var builder = EmbedCreateSpec.builder().title("Beets Clips").color(BeetsBot.COLOR);
    for (Clip clip : clips) {
      builder.addField(clip.name(), clip.title(), false);
    }
    return builder.build();
  }
}
