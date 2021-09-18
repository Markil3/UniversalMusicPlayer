/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer;

import edu.regis.universeplayer.PlaybackStatus;
import edu.regis.universeplayer.data.Song;

import java.io.Serializable;
import java.util.EventObject;

public class PlaybackInfo implements Serializable
{
    private final Song currentSong;
    /**
     * The time we are currently at in the song, in seconds.
     */
    private final float playTime;
    
    /**
     * The current status of the player.
     */
    private final PlaybackStatus status;
    
    public PlaybackInfo(Song song, float playTime, PlaybackStatus status)
    {
        this.currentSong = song;
        this.playTime = playTime;
        this.status = status;
    }
    
    /**
     * Obtains the song currently playing.
     *
     * @return The current song, or null if none is loaded.
     */
    public Song getSong()
    {
        return this.currentSong;
    }
    
    /**
     * Obtains the current play time.
     *
     * @return The play time of the player. This will be zero if the song is
     * stopped or not loaded.
     */
    public float getPlayTime()
    {
        return this.playTime;
    }
    
    /**
     * Obtains the current status of the player.
     *
     * @return - The player's playback status.
     */
    public PlaybackStatus getStatus()
    {
        return this.status;
    }

    @Override
    public String toString()
    {
        return "PlaybackInfo{" +
                "playTime=" + playTime +
                ", status=" + status +
                ", currentSong=" + currentSong +
                '}';
    }
}
