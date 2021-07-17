/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

/**
 * Lists all of the type of collections possible.
 */
public enum CollectionType
{
    album(Album.class), artist(String.class), albumArtist(String.class), genre(String.class), year(Integer.class);

    /**
     * The object class for this type.
     */
    public Class<?> objectType;

    /**
     * Creates a collection type.
     *
     * @param type - The class this type uses.
     */
    CollectionType(Class<?> type)
    {
        this.objectType = type;
    }
}
