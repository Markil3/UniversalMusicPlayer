/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

import java.util.EventListener;

/**
 * An update listener is called when an update is sent through the message
 * runner that is not associated with a message.
 *
 * @author William Hubbard
 * @version 0.1
 */
public interface UpdateListener extends EventListener
{
    /**
     * Called when an update is received from a message runner.
     *
     * @param object - The received object
     * @param runner - The runner that received the object.
     */
    void onUpdate(Object object, MessageRunner runner);
}
