package com.mattmerr.beets;

import com.google.common.collect.ImmutableSet;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.GuildChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.Set;

public class AllowedChannelResolver {

  private static final Logger log =
      LoggerFactory.getLogger(AllowedChannelResolver.class);

  public final Set<String> allowedChannels;
  public final Set<String> restrictedGuilds;

  AllowedChannelResolver(GatewayDiscordClient client) {
    String allowedChannel = System.getenv("BEETS_ALLOWED_CHANNEL");
    if (allowedChannel == null || allowedChannel.isEmpty()) {
      log.warn(
          "NO BEETS_ALLOWED_CHANNEL DEFINED! NO BEETS_ALLOWED_CHANNEL DEFINED!");
      log.warn(
          "NO BEETS_ALLOWED_CHANNEL DEFINED! NO BEETS_ALLOWED_CHANNEL DEFINED!");
      log.warn(
          "NO BEETS_ALLOWED_CHANNEL DEFINED! NO BEETS_ALLOWED_CHANNEL DEFINED!");
      allowedChannels = ImmutableSet.of();
      restrictedGuilds = ImmutableSet.of();
    } else {
      allowedChannels = ImmutableSet.copyOf(allowedChannel.split(","));
      restrictedGuilds = Flux.fromStream(allowedChannels.stream())
          .flatMap(channelId -> client.getChannelById(Snowflake.of(channelId)))
          .filter(channel -> channel instanceof GuildChannel)
          .map(channel -> ((GuildChannel) channel).getGuild())
          .filterWhen(guildMaybe -> guildMaybe.hasElement())
          .flatMap(guild -> guild)
          .map(guild -> guild.getId().asString())
          .collect(ImmutableSet.toImmutableSet())
          .retry(3)
          .block();
    }
  }
}
