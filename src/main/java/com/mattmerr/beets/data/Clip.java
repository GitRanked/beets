package com.mattmerr.beets.data;

import java.util.regex.Pattern;

public record Clip(String guildId, String name, String beet, String title) {

  public static final Pattern VALID_CLIP_NAME = Pattern.compile("[a-z]{3,20}");

}
