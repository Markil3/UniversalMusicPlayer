/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

/**
 * This query asks for the current playback time.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class QueryTime implements BrowserQuery<Float>
{
    @Override
    public String getCommandName()
    {
        return "currentTime";
    }
    
    @Override
    public Class<Float> getReturnType()
    {
        return Float.class;
    }
}
