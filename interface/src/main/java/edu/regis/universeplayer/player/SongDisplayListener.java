/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import java.util.Collection;
import java.util.EventListener;

import edu.regis.universeplayer.data.CollectionType;
import edu.regis.universeplayer.data.Song;

/**
 * A callback that is triggered whenever one element believes that the interface center display needs to be updated.
 *
 * @author William Hubbard
 * @version 0.1
 */
public interface SongDisplayListener extends EventListener
{
    /**
     * Called to display a list of songs, sorted by album.
     *
     * @param songs - The songs to display.
     */
    void updateSongs(Collection<Song> songs);

    /**
     * Called to display a list of collections
     *
     * @param type        - The type of collections to display.
     * @param collections - The collections to display
     */
    void updateCollections(CollectionType type, Collection<?> collections);
}
