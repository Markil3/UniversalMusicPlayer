/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer;

import edu.regis.universeplayer.data.PlaybackEvent;

import java.util.EventListener;

/**
 * A playback listener allows a class to listen for events regarding player
 * updates.
 *
 * @author William Hubbard
 * @version 0.1
 */
public interface PlaybackListener extends EventListener
{
    /**
     * Called when playback is changed.
     *
     * @param status - The playback status.
     */
    void onPlaybackChanged(PlaybackEvent status);
    
}
