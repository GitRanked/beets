package com.mattmerr.beets.vc;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

public class CompletableAudioLoader extends CompletableFuture<List<AudioTrack>> implements
    AudioLoadResultHandler {

  private final String resource;
  private final Boolean loadFullPlaylists;
  
  String message = null;
  List<AudioTrack> tracks = null;
  
  public CompletableAudioLoader(String resource, boolean loadFullPlaylists) {
    this.resource = resource;
    this.loadFullPlaylists = loadFullPlaylists;
  }
    
  @Override
  public void trackLoaded(AudioTrack track) {
    this.message = "Loaded: " + resource; 
    this.tracks = Collections.singletonList(track);
    this.complete(this.tracks);
  }

  @Override
  public void playlistLoaded(AudioPlaylist playlist) {
    if (this.loadFullPlaylists) {
      this.message = "Loaded playlist of: " + resource;
      this.tracks = playlist.getTracks();
    } else {
      AudioTrack selectedTrack = playlist.getSelectedTrack();
      if (selectedTrack == null) {
        selectedTrack = playlist.getTracks().get(0);
      }
      this.message = "Loaded selected track from: " + resource;
      this.tracks = Collections.singletonList(selectedTrack);
    }
    this.complete(this.tracks);
  }

  @Override
  public void noMatches() {
    this.message = "No match found for: " + resource;
    this.tracks = null;
    this.completeExceptionally(new NoSuchElementException(this.message));
  }

  @Override
  public void loadFailed(FriendlyException exception) {
    this.message = "Error loading: " + exception.getMessage();
    this.tracks = null;
    this.completeExceptionally(exception);
  }

  public String getResult() {
    return message;
  }

  public List<AudioTrack> getTracks() {
    return tracks;
  }
}
