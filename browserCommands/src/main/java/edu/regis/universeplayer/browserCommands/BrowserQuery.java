/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

import java.io.Serializable;

/**
 * Objects of this type are used by the interface to request information from
 * the browser.
 *
 * @author William Hubbard
 * @version 0.1
 */
public interface BrowserQuery<T> extends Serializable
{
    /**
     * Obtains the name of the command.
     * @return The command name.
     */
    String getCommandName();
    
    /**
     * Obtains the type of value this command returns.
     * @return The return type.
     */
    Class<T> getReturnType();
}
