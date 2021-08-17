/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

/**
 * The quit command tells the browser to shut down.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class CommandQuit implements BrowserCommand
{
    public CommandQuit()
    {
    
    }
    
    @Override
    public String getCommandName()
    {
        return "quit";
    }
}
