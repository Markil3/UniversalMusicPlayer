/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

import java.net.URL;

import edu.regis.universeplayer.data.InternetSong;

/**
 * Asks the browser for information on a certain song.
 *
 * @author William Hubbard
 * @version 0.2
 */
public class QuerySongData implements BrowserQuery<InternetSong>
{
    /**
     * The song to get data for.
     */
    private URL url;

    /**
     * For serialization only. Do not use.
     */
    public QuerySongData()
    {

    }

    /**
     * Creates a song data request.
     *
     * @param url - The URL to request data for.
     */
    public QuerySongData(URL url)
    {
        this.url = url;
    }

    /**
     * Obtains the name of the command.
     *
     * @return The command name.
     */
    @Override
    public String getCommandName()
    {
        return "getSongData";
    }

    /**
     * Obtains the type of value this command returns.
     *
     * @return The return type.
     */
    @Override
    public Class<InternetSong> getReturnType()
    {
        return InternetSong.class;
    }

    /**
     * Gets the location of the song to query.
     *
     * @return The song location.
     */
    public URL getUrl()
    {
        return this.url;
    }
}
