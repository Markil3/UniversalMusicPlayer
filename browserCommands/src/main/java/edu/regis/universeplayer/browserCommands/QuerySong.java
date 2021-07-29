/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

import java.net.URL;

/**
 * This query asks for the currently loaded song.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class QuerySong implements BrowserQuery<URL>
{
    @Override
    public String getCommandName()
    {
        return "getSong";
    }
    
    @Override
    public Class<URL> getReturnType()
    {
        return URL.class;
    }
}
