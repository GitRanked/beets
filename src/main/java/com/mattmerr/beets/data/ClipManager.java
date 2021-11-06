package com.mattmerr.beets.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mattmerr.beets.commands.CommandDesc.Option;
import com.mattmerr.beets.data.SqliteModule.BeetsDB;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ClipManager {
  
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private final Connection conn;

  @Inject
  public ClipManager(@BeetsDB Connection conn) {
    this.conn = conn;
  }
  
  public void insertClip(String guildId, String name, String payload) throws SQLException {
    log.info("INSERT " + name);
    var prep = conn.prepareStatement(
        "INSERT INTO clips (guild, name, payload) VALUES (?, ?, ?)");
    prep.setString(1, guildId);
    prep.setString(2, name);
    prep.setString(3, payload);
    prep.execute();
  }
  
  public void upsertClip(String guildId, String name, String payload) throws SQLException {
    log.info("UPSERT " + name);
    var prep = conn.prepareStatement(
        "INSERT INTO clips (guild, name, payload) VALUES (?, ?, ?)" 
            + " ON CONFLICT DO UPDATE SET payload=excluded.payload");
    prep.setString(1, guildId);
    prep.setString(2, name);
    prep.setString(3, payload);
    prep.execute();
  }

  public Optional<String> selectClip(String guildId, String name) throws SQLException {
    log.info("SELECT " + name);
    var prep = conn.prepareStatement(
        "SELECT payload FROM clips WHERE guild=? AND name=?");
    prep.setString(1, guildId);
    prep.setString(2, name);
    
    try (var result = prep.executeQuery()) {
      if (result.next()) {
        return Optional.of(result.getString(1));
      }
      return Optional.empty();
    }
  }

}
