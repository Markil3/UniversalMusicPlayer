/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer;

import edu.regis.universeplayer.data.Song;

import java.util.EventListener;
import java.util.EventObject;

/**
 * A playback listener allows a class to listen for events regarding player
 * updates.
 *
 * @author William Hubbard
 * @verison 0.1
 */
public interface PlaybackListener extends EventListener
{
    /**
     * Called when playback is changed.
     *
     * @param status - The playback status.
     */
    void onPlaybackChanged(PlaybackInfo status);
    
    class PlaybackInfo extends EventObject
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
        
        public PlaybackInfo(Player<? extends Song> player, Song song, float playTime, PlaybackStatus status)
        {
            super(player);
            this.currentSong = song;
            this.playTime = playTime;
            this.status = status;
        }
        
        @Override
        public Player<? extends Song> getSource()
        {
            return (Player<? extends Song>) super.getSource();
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
    }
}
