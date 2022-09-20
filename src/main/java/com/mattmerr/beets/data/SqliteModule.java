package com.mattmerr.beets.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static java.lang.String.format;

public class SqliteModule {

  private final Logger log = LoggerFactory.getLogger(SqliteModule.class);
  private final Path dbPath;
  public final Connection conn;

  public SqliteModule(Path dbPath) {
    this.dbPath = dbPath;
    try {
      Class.forName("org.sqlite.JDBC");
      Connection conn =
          DriverManager.getConnection("jdbc:sqlite:" + dbPath.toString());
      Statement statement = conn.createStatement();
      tryExecute(
          statement,
          "create clips",
          "CREATE TABLE IF NOT EXISTS clips ("
              + " guild STRING,"
              + " name STRING,"
              + " beet STRING,"
              + " title STRING,"
              + " create_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,"
              + " PRIMARY KEY (guild, name)"
              + ")");
      this.conn = conn;
    } catch (SQLException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private void tryExecute(Statement statement, String name, String sql) {
    try {
      statement.executeUpdate(sql);
    } catch (SQLException e) {
      log.error(format("Cannot apply ALTER \"%s\"", name), e);
    }
  }
}
