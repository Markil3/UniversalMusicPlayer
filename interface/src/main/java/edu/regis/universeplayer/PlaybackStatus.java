/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer;

/**
 * This contains possible states that a player can be in.
 */
public enum PlaybackStatus
{
    /**
     * The player is currently playing a song.
     */
    PLAYING,
    /**
     * The player has been paused.
     */
    PAUSED,
    /**
     * The player has a song loaded, but is not playing it.
     */
    STOPPED,
    /**
     * No song is loaded.
     */
    EMPTY;
}
