/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import java.util.Collection;

/**
 * The song provider interface serves to give the application access to any form of song database as needed.
 *
 * @author William Hubbard
 * @version 0.1
 */
public interface SongProvider
{
    /**
     * A SongProvider instance designed to
     */
    CompiledSongProvider INSTANCE = new CompiledSongProvider(new SimpleSongProvider());

    /**
     * Obtains all albums within the collection.
     *
     * @return A list of albums.
     */
    Collection<Album> getAlbums();

    /**
     * Obtains all songs within the collection.
     *
     * @return A list of songs.
     */
    Collection<Song> getSongs();

    /**
     * Obtains a list of all artists.
     *
     * @return All artists.
     */
    Collection<String> getArtists();

    /**
     * Obtains a list of all album artists.
     *
     * @return All album artists.
     */
    Collection<String> getAlbumArtists();

    /**
     * Obtains a list of all genres.
     *
     * @return All genres.
     */
    Collection<String> getGenres();

    /**
     * Obtains a list of all years that have albums.
     *
     * @return All years.
     */
    Collection<Integer> getYears();

    /**
     * Obtains all songs from an album.
     *
     * @param album - The album to obtain
     * @return All songs from the requested album, or null if that album is not in the database.
     */
    Collection<Song> getSongsFromAlbum(Album album);

    /**
     * Obtains all songs written by a given artist.
     *
     * @param artist - The artist to search for
     * @return A list of all songs from the specified artist, or null if that artist is not in the
     * database.
     */
    Collection<Song> getSongsFromArtist(String artist);

    /**
     * Obtains an album by a specific name.
     *
     * @param name - The name to search for.
     * @return - The first album that matches the given name, or null if that album name is not in
     * the database.
     */
    Album getAlbumByName(String name);

    /**
     * Obtains all albums that were written by a certain artist.
     *
     * @param artist - The artist to search for.
     * @return - The collection on matching albums.
     */
    Collection<Album> getAlbumsFromArtist(String artist);

    /**
     * Obtains all albums that match a certain genre
     *
     * @param genre - The genre to search for.
     * @return - The collection on matching albums.
     */
    Collection<Album> getAlbumsFromGenre(String genre);

    /**
     * Obtains all albums that were released a certain year.
     *
     * @param year - The year to search for.
     * @return - The collection on matching albums.
     */
    Collection<Album> getAlbumsFromYear(int year);
}
