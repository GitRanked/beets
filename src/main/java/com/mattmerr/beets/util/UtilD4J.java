package com.mattmerr.beets.util;

import com.mattmerr.beets.BeetsBot;
import com.mattmerr.beets.util.RepliableEventException.MissingGuildException;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import java.time.Instant;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class UtilD4J {
  
  public static Optional<String> asString(Optional<ApplicationCommandInteractionOption> option) {
    return option
        .flatMap(ApplicationCommandInteractionOption::getValue)
        .map(ApplicationCommandInteractionOptionValue::asString);
  }
  
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public static String asRequiredString(Optional<ApplicationCommandInteractionOption> option) {
    return asString(option).get();
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public static Optional<Long> asLong(Optional<ApplicationCommandInteractionOption> option) {
    return option
        .flatMap(ApplicationCommandInteractionOption::getValue)
        .map(ApplicationCommandInteractionOptionValue::asLong);
  }
  
  public static Snowflake requireGuildId(Interaction interaction) {
    return interaction.getGuildId().orElseThrow(MissingGuildException::new);
  }
  
  public static EmbedCreateSpec.Builder beetsEmbedBuilder(String title) {
    return EmbedCreateSpec.builder()
        .color(BeetsBot.COLOR)
        .title(title);
  }
  
  public static EmbedCreateSpec simpleMessageEmbed(String title, String description) {
    return beetsEmbedBuilder(title).description(description).build();
  }
  
  public static InteractionApplicationCommandCallbackSpec wrapEmbedReply(EmbedCreateSpec spec) {
    return InteractionApplicationCommandCallbackSpec.builder()
        .addEmbed(spec)
        .build();
  }

}
