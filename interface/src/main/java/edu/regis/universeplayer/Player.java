/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer;

import edu.regis.universeplayer.browserCommands.QueryFuture;
import edu.regis.universeplayer.data.Song;

import java.util.concurrent.Future;

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
    QueryFuture<Void> loadSong(T song);
    
    /**
     * Enables playback of the current song, if one is active.
     * @return A confirmation of whether the command was successful or not.
     */
    QueryFuture<Void> play();
    
    /**
     * Pauses playback of the current song.
     * @return A confirmation of whether the command was successful or not.
     */
    QueryFuture<Void> pause();
    
    /**
     * Toggles between playing and pausing the current song.
     * @return A confirmation of whether the command was successful or not.
     */
    QueryFuture<Void> togglePlayback();
    
    /**
     * Sets the current song time to the specified position.
     *
     * @param time - The specified time in the song, in seconds.
     * @return A confirmation of whether the command was successful or not.
     */
    QueryFuture<Void> seek(float time);
    
    /**
     * Checks to see if the song is paused.
     *
     * @return Whether or not the song is paused.
     */
    QueryFuture<Boolean> isPaused();
    
    /**
     * Obtains the time we are currently at in the current song.
     *
     * @return - The current song position in seconds, or -1 if no song is playing.
     */
    QueryFuture<Float> getCurrentTime();
    
    /**
     * Gets the length of the current song.
     *
     * @return The song length, in seconds.
     */
    QueryFuture<Float> getLength();
    
    /**
     * Closes the player.
     * @return A confirmation of whether the player was successfully closed.
     */
    QueryFuture<Void> close();
}
