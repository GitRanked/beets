package com.mattmerr.beets.vc;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

public class CompletableAudioLoader extends CompletableFuture<AudioTrack> implements
    AudioLoadResultHandler {

  private final String resource;
  
  String message = null;
  AudioTrack track = null;
  
  public CompletableAudioLoader(String resource) {
    this.resource = resource;
  }
    
  @Override
  public void trackLoaded(AudioTrack track) {
    this.message = "Loaded: " + resource; 
    this.track = track;
    this.complete(this.track);
  }

  @Override
  public void playlistLoaded(AudioPlaylist playlist) {
    AudioTrack firstTrack = playlist.getSelectedTrack();

    if (firstTrack == null) {
      firstTrack = playlist.getTracks().get(0);
    }

    this.message = "Loaded first track of: " + resource;
    this.track = firstTrack;
    this.complete(this.track);
  }

  @Override
  public void noMatches() {
    this.message = "No match found for: " + resource;
    this.track = null;
    this.completeExceptionally(new NoSuchElementException(this.message));
  }

  @Override
  public void loadFailed(FriendlyException exception) {
    this.message = "Error loading: " + exception.getMessage();
    this.track = null;
    this.completeExceptionally(exception);
  }

  public String getResult() {
    return message;
  }

  public AudioTrack getTrack() {
    return track;
  }
}
