package com.mattmerr.beets.util;

public class NotInVoiceChatException extends RepliableMessageException {
    public NotInVoiceChatException() {
      super("You must join a Voice Channel!");
    }
  }