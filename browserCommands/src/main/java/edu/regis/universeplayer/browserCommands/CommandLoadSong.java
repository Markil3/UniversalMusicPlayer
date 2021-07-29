/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This command is used to tell the browser to load a song URL, such as a
 * YouTube video.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class CommandLoadSong implements BrowserCommand
{
    /**
     * The song to load.
     */
    private URL song;
    
    /**
     * Used for serialization only. Do not use.
     */
    public CommandLoadSong()
    {
    
    }
    
    /**
     * Tells the browser to load a song.
     *
     * @param song - The song to load.
     */
    public CommandLoadSong(URL song)
    {
        this.song = song;
    }
    
    /**
     * Tells the browser to load a song.
     *
     * @param song - The song to load.
     */
    public CommandLoadSong(String song) throws MalformedURLException
    {
        this.song = new URL(song);
    }
    
    @Override
    public String getCommandName()
    {
        return "loadSong";
    }
    
    /**
     * Obtains the song that is to be loaded.
     *
     * @return - The loaded song.
     */
    public URL getSong()
    {
        return this.song;
    }
}
