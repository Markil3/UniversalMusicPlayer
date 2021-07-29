/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

/**
 * This command sets the playback status of the browser.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class CommandSetPlayback implements BrowserCommand
{
    /**
     * An enum for playback status.
     */
    public enum Playback
    {
        PLAY, PAUSE
    }
    
    /**
     * The playback status communicated by the interface.
     */
    private Playback status;
    
    /**
     * Creates a play command.
     * @return A new play command.
     */
    public static CommandSetPlayback playCommand()
    {
        return new CommandSetPlayback(Playback.PLAY);
    }
    
    /**
     * Creates a pause command.
     * @return A new pause command.
     */
    public static CommandSetPlayback pauseCommand()
    {
        return new CommandSetPlayback(Playback.PAUSE);
    }
    
    /**
     * Used for serialization only. Do not use.
     */
    public CommandSetPlayback()
    {
    
    }
    
    /**
     * Tells the browser to chang the playback.
     *
     * @param status - Whether the browser should be playing or pausing.
     */
    public CommandSetPlayback(Playback status)
    {
        this.status = status;
    }
    
    @Override
    public String getCommandName()
    {
        return "playback";
    }
    
    /**
     * Obtains the playback command.
     *
     * @return Whether the browser should be playing or not.
     */
    public Playback getPlayback()
    {
        return this.status;
    }
    
    /**
     * Checks whether this command sets the browser playing.
     *
     * @return Whether the browser should be playing or not.
     */
    public boolean shouldPlay()
    {
        return this.status == Playback.PLAY;
    }
    
    /**
     * Checks whether this command pauses
     *
     * @return Whether the browser should be playing or not.
     */
    public boolean shouldPause()
    {
        return this.status == Playback.PAUSE;
    }
}
