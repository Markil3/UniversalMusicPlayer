/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import java.io.File;
import java.util.Collection;

/**
 * The song provider interface serves to give the application access to any form of song database as needed.
 *
 * @author William Hubbard
 * @version 0.1
 */
public interface SongProvider<T extends Song>
{
    /**
     * A SongProvider instance designed to
     */
    SongProvider<Song> INSTANCE = new CompiledSongProvider(new LocalSongProvider(new File(System.getProperty("user.home"), "Music")), InternetSongProvider.getInstance());
    
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
    Collection<T> getSongs();
    
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
    Collection<T> getSongsFromAlbum(Album album);
    
    /**
     * Obtains all songs written by a given artist.
     *
     * @param artist - The artist to search for
     * @return A list of all songs from the specified artist, or null if that artist is not in the
     * database.
     */
    Collection<T> getSongsFromArtist(String artist);
    
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
    
    /**
     * Checks to see if we are updating any songs.
     *
     * @return True if we are updating the song cache.
     */
    default boolean isUpdating()
    {
        return this.getTotalUpdateSongs() != 0;
    }
    
    /**
     * When we are updating the song cache, this method obtains the number of
     * songs already updated.
     *
     * @return The number of songs successfully updated.
     */
    int getUpdateProgress();
    
    /**
     * Determines whether or not we are updating the cache and, if so, gets the
     * total number of songs we need to update
     *
     * @return The total number of songs to update. A 0 means that we do not
     * have any songs to update, and a negative number means that we are
     * currently calculating how many songs we need to update.
     */
    int getTotalUpdateSongs();
    
    /**
     * Determines the text displayed for the progress of updates
     *
     * @return The update status text.
     */
    String getUpdateText();
    
    /**
     * Adds a listener for song updates.
     *
     * @param listener - The listener to add.
     */
    void addUpdateListener(UpdateListener listener);
    
    /**
     * Removes a listener for song updates.
     *
     * @param listener - The listener to remove.
     */
    void removeUpdateListener(UpdateListener listener);
}
