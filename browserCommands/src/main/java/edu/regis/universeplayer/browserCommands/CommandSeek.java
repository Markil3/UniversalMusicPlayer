/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

/**
 * The seek command tells the browser to move playback to a certain location.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class CommandSeek implements BrowserCommand
{
    /**
     * The time to seek to.
     */
    private float time;
    /**
     * Whether the {@link #time} parameter should be interpreted as relative to
     * the current position or absolute.
     */
    private boolean relative;
    
    /**
     * For serialization only. Do not use.
     */
    public CommandSeek()
    {
    
    }
    
    /**
     * Tells the browser to seek to a certain absolute time in the song.
     *
     * @param time - The time to seek to.
     */
    public CommandSeek(float time)
    {
        this.time = time;
        this.relative = false;
    }
    
    /**
     * Tells the browser to seek to a certain time in the song.
     *
     * @param time     - The time to seek to.
     * @param relative - Whether or not the time should be treated as relative
     *                 to the current play time or not.
     */
    public CommandSeek(float time, boolean relative)
    {
        this.time = time;
        this.relative = relative;
    }
    
    @Override
    public String getCommandName()
    {
        return "seek";
    }
    
    /**
     * Obtains the time to seek to.
     *
     * @return The new playback time.
     */
    public float getSeekTime()
    {
        return this.time;
    }
    
    /**
     * Checks to see whether the command should be interpreted as local to the
     * current play time.
     *
     * @return Whether the seek time is local or not.
     */
    public boolean isRelative()
    {
        return this.relative;
    }
}
