/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import edu.regis.universeplayer.PlaybackListener;
import edu.regis.universeplayer.PlaybackStatus;
import edu.regis.universeplayer.browserCommands.QueryFuture;
import edu.regis.universeplayer.data.Song;

import java.util.HashMap;
import java.util.concurrent.ForkJoinTask;

/**
 * This interface serves as the connection to a music player of some sort, whether it be
 * browser-based or from a file.
 *
 * @param <T> - The type of songs supported.
 * @author William Hubbard
 * @since 0.1
 */
public interface Player<T extends Song>
{
    /**
     * Obtains the song currently playing.
     *
     * @return The current song, or null if none is playing.
     */
    Song getCurrentSong();
    
    /**
     * Loads up a song
     *
     * @param song - The song to load.
     * @return A confirmation of whether the command was successful or not.
     */
    ForkJoinTask<Void> loadSong(T song);
    
    /**
     * Enables playback of the current song, if one is active.
     *
     * @return A confirmation of whether the command was successful or not.
     */
    ForkJoinTask<Void> play();
    
    /**
     * Pauses playback of the current song.
     *
     * @return A confirmation of whether the command was successful or not.
     */
    ForkJoinTask<Void> pause();
    
    /**
     * Toggles between playing and pausing the current song.
     *
     * @return A confirmation of whether the command was successful or not.
     */
    ForkJoinTask<Void> togglePlayback();
    
    /**
     * Stops playback of the current song.
     */
    ForkJoinTask<Void> stopSong();
    
    /**
     * Sets the current song time to the specified position.
     *
     * @param time - The specified time in the song, in seconds.
     * @return A confirmation of whether the command was successful or not.
     */
    ForkJoinTask<Void> seek(float time);
    
    /**
     * Obtains the player's current playback status.
     * @return A future for the request.
     */
    ForkJoinTask<PlaybackStatus> getStatus();
    
    /**
     * Obtains the time we are currently at in the current song.
     *
     * @return - The current song position in seconds, or -1 if no song is playing.
     */
    ForkJoinTask<Float> getCurrentTime();
    
    /**
     * Gets the length of the current song.
     *
     * @return The song length, in seconds.
     */
    ForkJoinTask<Float> getLength();
    
    /**
     * Closes the player.
     *
     * @return A confirmation of whether the player was successfully closed.
     */
    ForkJoinTask<Void> close();
    
    /**
     * Adds a listener for playback status updates.
     *
     * @param listener - The listener to add.
     */
    void addPlaybackListener(PlaybackListener listener);
    
    /**
     * Checks to see if a listener has been added.
     *
     * @param listener - The listener to look for.
     * @return True if the provided listener has been added, false otherwise.
     */
    boolean hasPlaybackListener(PlaybackListener listener);
    
    /**
     * Removes a listener for playback status updates.
     *
     * @param listener - The listener to remove.
     */
    void removePlaybackListener(PlaybackListener listener);
}
