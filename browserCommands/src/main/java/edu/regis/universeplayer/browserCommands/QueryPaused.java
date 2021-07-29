/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

/**
 * This query asks for confirmation as to whether the browser playback is paused
 * or not.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class QueryPaused implements BrowserQuery<Boolean>
{
    @Override
    public String getCommandName()
    {
        return "isPaused";
    }
    
    @Override
    public Class<Boolean> getReturnType()
    {
        return Boolean.class;
    }
}
