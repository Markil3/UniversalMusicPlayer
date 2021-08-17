/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

/**
 * This query asks for whether a song is currently playing or not. Note that
 * this is not the same as whether it is paused or not, as a song could have
 * run into a playback error.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class QueryStatus implements BrowserQuery<Integer>
{
    @Override
    public String getCommandName()
    {
        return "getStatus";
    }
    
    @Override
    public Class<Integer> getReturnType()
    {
        return Integer.class;
    }
}
