package com.mattmerr.beets.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.vc.VCManager;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.PartialMember;
import discord4j.rest.util.ApplicationCommandOptionType;
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

  @Inject
  PlayCommand(VCManager vcManager) {
    this.vcManager = vcManager;
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
    logCall(event);
    String beet = event.getOption("beet").get().getValue().get().asString();
    return event.getInteraction().getGuild().flatMap(
        guild -> guild.getMemberById(event.getInteraction().getUser().getId())
            .flatMap(PartialMember::getVoiceState)
            .flatMap(VoiceState::getChannel)
            .flatMap(vc -> vcManager.enqueue(event, vc, beet)))
//        .then(event.reply("Joined VC!"))
        .doOnError(e -> log.error("Error processing Play", e))
        .onErrorResume(e -> event.reply("Error trying to Play!"));
//        return Mono.justOrEmpty(event.getOption(ARG_TARGET_URL))
//            .doOnNext(option -> playerManager.loadItem(
//                option.getValue().get().asString(),
//                scheduler)).then();
  }

}
