package com.mattmerr.beets.commands;

import static discord4j.core.event.EventDispatcher.log;
import static discord4j.core.event.EventDispatcher.replayingWithTimeout;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.vc.VCManager;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.PartialMember;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

@CommandDesc(
    name = "skip",
    description = "Skip a beet",
    options = {}
)
@Singleton
public class SkipCommand extends CommandBase {

  private final VCManager vcManager;

  @Inject
  SkipCommand(VCManager vcManager) {
    this.vcManager = vcManager;
  }

  @Override
  public Mono<Void> execute(SlashCommandEvent event) {
    logCall(event);
    return event.getInteraction().getGuild().flatMap(
        guild -> guild.getMemberById(event.getInteraction().getUser().getId())
            .flatMap(PartialMember::getVoiceState)
            .flatMap(VoiceState::getChannel)
            .map(vcManager::getSessionOrNull)
            .map(session -> {
              if (session == null) {
                return "Nothing to skip!";
              }
              session.skip();
              return "Skipped!";
            })
            .flatMap(event::reply))
//        .then(event.reply("Joined VC!"))
        .doOnError(e -> log.error("Error processing Skip", e))
        .onErrorResume(e -> event.reply("Error trying to Skip!"));
//        return Mono.justOrEmpty(event.getOption(ARG_TARGET_URL))
//            .doOnNext(option -> playerManager.loadItem(
//                option.getValue().get().asString(),
//                scheduler)).then();
  }

}
