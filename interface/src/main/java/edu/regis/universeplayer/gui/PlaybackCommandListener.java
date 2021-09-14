/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.gui;

import java.util.EventListener;

/**
 * A listener that allows objects to observe when playback commands are triggered.
 *
 * @author William Hubbard
 * @version 0.1
 */
public interface PlaybackCommandListener extends EventListener
{
    /**
     * Called when a playback command is issued.
     *
     * @param command - The command issued.
     * @param data    - Additional data relevent to the command.
     */
    void onCommand(PlaybackCommand command, Object data);
}
