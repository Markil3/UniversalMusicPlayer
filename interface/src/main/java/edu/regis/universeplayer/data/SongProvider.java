/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import java.io.File;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The song provider interface serves to give the application access to any form of song database as needed.
 *
 * @author William Hubbard
 * @version 0.1
 */
public interface SongProvider<T extends Song> extends DataProvider<T>
{
//    SongProvider<Song> INSTANCE = new CompiledSongProvider(new LocalSongProvider(new File(System.getProperty("user.home"), "Music")), InternetSongProvider.getInstance());

    /**
     * Obtains a reference to the album provider.
     * @return The album provider used.
     */
    AlbumProvider getAlbumProvider();

    /**
     * Obtains all albums within the collection.
     *
     * @return A list of albums.
     */
    default Collection<Album> getAlbums()
    {
        return this.getCollection().stream().map(s -> s.album).collect(Collectors
                .toSet());
    }

    /**
     * Obtains all songs within the collection.
     *
     * @return A list of songs.
     */
    default Collection<T> getSongs()
    {
        return this.getCollection();
    }

    /**
     * Obtains a list of all artists.
     *
     * @return All artists.
     */
    default Collection<String> getArtists()
    {
        return this.getCollection().stream().filter(s -> s.artists != null).map(s -> s.artists)
                   .mapMulti((BiConsumer<String[], Consumer<String>>) (strings, objectConsumer) -> {
                       for (String string : strings)
                       {
                           objectConsumer.accept(string);
                       }
                   }).collect(Collectors.toSet());
    }

    /**
     * Obtains all songs from an album.
     *
     * @param album - The album to obtain
     * @return All songs from the requested album, or null if that album is not
     * in the database.
     */
    default Collection<T> getSongsFromAlbum(Album album)
    {
        return this.getCollection().stream().filter(s -> s.album == album)
                   .collect(Collectors.toSet());
    }

    /**
     * Obtains all songs written by a given artist.
     *
     * @param artist - The artist to search for
     * @return A list of all songs from the specified artist, or null if that
     * artist is not in the database.
     */
    default Collection<T> getSongsFromArtist(String artist)
    {
        return this.getCollection().stream().filter(s -> {
            for (int i = 0; i < s.artists.length; i++)
            {
                if (s.artists[i].equals(artist))
                {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toSet());
    }
}
