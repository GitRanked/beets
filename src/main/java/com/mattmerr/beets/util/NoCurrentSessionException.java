package com.mattmerr.beets.util;

public class NoCurrentSessionException extends RepliableMessageException {
  public NoCurrentSessionException() {
    super("There is not currently a session!");
  }
}