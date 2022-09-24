package com.mattmerr.beets.util;

public class SessionInDifferentVCException extends RepliableMessageException {

  public static final String MESSAGE =
      "Beets is already playing in a different VC!";

  public SessionInDifferentVCException() {
    super(MESSAGE);
  }
}
