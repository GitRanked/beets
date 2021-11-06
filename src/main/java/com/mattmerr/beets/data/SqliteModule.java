package com.mattmerr.beets.data;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Singleton;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Singleton
public class SqliteModule extends AbstractModule {

  private final Path dbPath;

  public SqliteModule(Path dbPath) {
    this.dbPath = dbPath;
  }
  
  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  public @interface BeetsDB {}

  @Override
  protected void configure() {
    try {
      Class.forName("org.sqlite.JDBC");
      Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toString());
      Statement statement = conn.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS clips (guild STRING, name STRING, payload STRING, create_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL)");
      bind(Connection.class).annotatedWith(BeetsDB.class).toInstance(conn);
      
      // do not close conn
    } catch (SQLException | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
  
  
}
