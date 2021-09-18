/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

/**
 * A song update listener is used to tell others when a song provider starts
 * updating songs.
 *
 * @author William Hubbard
 * @version 0.1
 */
public interface UpdateListener
{
    /**
     * Called when the update status of the player has changed.
     * @param provider - The provider that triggered the listener.
     * @param updated - The number of songs updated.
     * @param totalUpdate - The total number of songs to update, or -1 if we are
     *                    still determining that.
     * @param updating - The text to display on update bars.
     */
    <T> void onUpdate(DataProvider<T> provider, int updated, int totalUpdate,
                  String updating);
}
