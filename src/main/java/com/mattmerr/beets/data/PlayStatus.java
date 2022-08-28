package com.mattmerr.beets.data;

import com.google.common.collect.ImmutableList;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public record PlayStatus(AudioTrack currentTrack,
                         ImmutableList<AudioTrack> queue) {}
