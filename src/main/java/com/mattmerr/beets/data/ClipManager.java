package com.mattmerr.beets.data;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.data.SqliteModule.BeetsDB;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@Singleton
public class ClipManager {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private final Connection conn;
  private final AudioPlayerManager playerManager;

  @Inject
  public ClipManager(@BeetsDB Connection conn) {
    this.conn = conn;

    this.playerManager = new DefaultAudioPlayerManager();
    playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
    AudioSourceManagers.registerRemoteSources(playerManager);
  }

  public void upsertClip(Clip clip) throws SQLException {
    log.info("UPSERT " + clip.name());
    var prep =
        conn.prepareStatement(
            "INSERT INTO clips (guild, name, beet, title) VALUES (?, ?, ?, ?)"
                + " ON CONFLICT DO UPDATE SET beet=excluded.beet, title=excluded.title");
    prep.setString(1, clip.guildId());
    prep.setString(2, clip.name());
    prep.setString(3, clip.beet());
    prep.setString(4, clip.title());
    prep.execute();
  }

  public Optional<Clip> selectClip(String guildId, String name) throws SQLException {
    log.info("SELECT " + name);
    var prep =
        conn.prepareStatement(
            "SELECT guild, name, beet, title FROM clips WHERE guild=? AND name=?");
    prep.setString(1, guildId);
    prep.setString(2, name);

    try (var result = prep.executeQuery()) {
      if (result.next()) {
        return Optional.of(
            new Clip(
                result.getString(1),
                result.getString(2),
                result.getString(3),
                result.getString(4)));
      }
      return Optional.empty();
    }
  }

  public ImmutableList<Clip> enumerateClips(String guildId, long page) throws SQLException {
    var prep =
        conn.prepareStatement(
            "SELECT name, beet, title FROM clips WHERE guild=? ORDER BY name ASC LIMIT 25 OFFSET ?");
    prep.setString(1, guildId);
    prep.setLong(2, page * 25);

    var result = prep.executeQuery();
    var listBuilder = ImmutableList.<Clip>builder();
    while (result.next()) {
      var name = result.getString(1);
      var beet = result.getString(2);
      var title = result.getString(3);
      listBuilder.add(new Clip(guildId, name, beet, title));
    }
    return listBuilder.build();
  }
}
