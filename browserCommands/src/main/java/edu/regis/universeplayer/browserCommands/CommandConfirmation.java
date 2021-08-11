/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

import java.io.Serializable;

/**
 * This class contains information as to the success of a command.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class CommandConfirmation implements Serializable
{
    /**
     * The error returned by the browser, or null if execution was successful.
     */
    private Throwable errorCode;
    private String message;
    
    /**
     * Confirms a successful command.
     */
    public CommandConfirmation()
    {
        this("Command successful");
    }
    
    /**
     * Confirms a successful command.
     *
     * @param message - Details on the nature of the command execution.
     */
    public CommandConfirmation(String message)
    {
        this((BrowserError) null);
        this.message = message;
    }
    
    /**
     * Confirms an unsuccessful command.
     *
     * @param error - The error thrown, or null if the command was successful.
     */
    public CommandConfirmation(Throwable error)
    {
        this.errorCode = error;
    }
    
    /**
     * Checks to see if the command executed properly.
     *
     * @return True if the command was successful, false otherwise.
     */
    public boolean wasSuccessful()
    {
        return this.errorCode == null;
    }
    
    /**
     * Obtains the error returned by the browser.
     *
     * @return The error, or null if the command was successful.
     */
    public Throwable getError()
    {
        return this.errorCode;
    }
    
    /**
     * Obtains details as to the nature of the command execution.
     *
     * @return The detailed message on the command.
     */
    public String getMessage()
    {
        return this.errorCode != null ? this.errorCode.getMessage() : this.message;
    }
}
