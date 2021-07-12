/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universe_player;

import edu.regis.universe_player.data.Song;

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
     */
    void play(T song);

    /**
     * Enables playback of the current song, if one is active.
     */
    void play();

    /**
     * Pauses playback of the current song.
     */
    void pause();

    /**
     * Toggles between playing and pausing the current song.
     */
    void togglePlayback();

    /**
     * Sets the current song time to the specified position.
     *
     * @param time - The specified time in the song, in seconds.
     */
    void seek(float time);

    /**
     * Checks to see if the song is paused.
     *
     * @return Whether or not the song is paused.
     */
    boolean isPaused();

    /**
     * Obtains the time we are currently at in the current song.
     *
     * @return - The current song position in seconds, or -1 if no song is playing.
     */
    float getCurrentTime();

    /**
     * Gets the length of the current song.
     *
     * @return The song length, in seconds.
     */
    float getLength();
}
