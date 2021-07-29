/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

/**
 * This query asks for the total length of the song.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class QueryLength implements BrowserQuery<Float>
{
    @Override
    public String getCommandName()
    {
        return "length";
    }
    
    @Override
    public Class<Float> getReturnType()
    {
        return Float.class;
    }
}
