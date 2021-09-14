/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

import java.io.Serializable;

/**
 * This wrapper class serves to return a browser return value over serialization protocols.
 *
 * @param <T> The type of return value.
 * @author William Hubbard
 * @version 0.1
 */
public class CommandReturn<T> implements Serializable
{
    private T returnValue;
    
    private CommandConfirmation confirmation;
    
    /**
     * For serialization only. Do not use.
     */
    public CommandReturn()
    {
    
    }
    
    /**
     * Creates a new return object.
     *
     * @param value        - The value to return.
     * @param confirmation - Information on the execution of the command.
     */
    public CommandReturn(T value, CommandConfirmation confirmation)
    {
        this.returnValue = value;
        this.confirmation = confirmation;
    }
    
    /**
     * Obtains the value returned.
     *
     * @return The returned value. May be null.
     */
    public T getReturnValue()
    {
        return this.returnValue;
    }
    
    /**
     * Obtains information about the execution of this command.
     *
     * @return The execution status.
     */
    public CommandConfirmation getConfirmation()
    {
        return this.confirmation;
    }

    @Override
    public String toString()
    {
        return "CommandReturn{" +
                "returnValue=" + returnValue +
                ", confirmation=" + confirmation +
                '}';
    }
}
