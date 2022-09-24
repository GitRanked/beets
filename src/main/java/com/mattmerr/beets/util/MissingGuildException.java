package com.mattmerr.beets.util;

public class MissingGuildException extends RepliableMessageException {
    public MissingGuildException() {
      super("This command must be run within a Guild!");
    }
  }