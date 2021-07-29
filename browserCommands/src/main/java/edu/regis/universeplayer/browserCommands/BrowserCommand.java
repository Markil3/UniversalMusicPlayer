/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

/**
 * Browser commands are used to send orders to the browser, requesting
 * confirmation as to whether those commands were completed or not.
 */
public interface BrowserCommand extends BrowserQuery<Void>
{
    @Override
    default Class<Void> getReturnType()
    {
        return Void.class;
    }
}
