/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

public class LocalSongProvider implements SongProvider
{
    private File source;

    private HashMap<File, Song> songs = new HashMap<>();

    private class SongScanner implements Runnable
    {
        @Override
        public void run()
        {

        }

        private void scanFolder(File folder)
        {
            String type;
            LinkedList<File> subFolders = new LinkedList<>();
            for (File file: folder.listFiles())
            {
                if (file.isDirectory())
                {
                    this.scanFolder(file);
                }
                if (file.getName().lastIndexOf(".") < file.getName().length() - 1)
                {
                    type = file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase();
                    switch (type)
                    {
                    case "mp3":

                    }
                }
            }
        }
    }

    public LocalSongProvider(File source)
    {
        this.source = source;
        if (this.source == null || !this.source.isDirectory())
        {
            throw new IllegalArgumentException("File source must be existing directory");
        }


    }

    /**
     * Obtains all albums within the collection.
     *
     * @return A list of albums.
     */
    @Override
    public Collection<Album> getAlbums()
    {
        return null;
    }

    /**
     * Obtains all songs within the collection.
     *
     * @return A list of songs.
     */
    @Override
    public Collection<Song> getSongs()
    {
        return null;
    }

    /**
     * Obtains a list of all artists.
     *
     * @return All artists.
     */
    @Override
    public Collection<String> getArtists()
    {
        return null;
    }

    /**
     * Obtains a list of all album artists.
     *
     * @return All album artists.
     */
    @Override
    public Collection<String> getAlbumArtists()
    {
        return null;
    }

    /**
     * Obtains a list of all genres.
     *
     * @return All genres.
     */
    @Override
    public Collection<String> getGenres()
    {
        return null;
    }

    /**
     * Obtains a list of all years that have albums.
     *
     * @return All years.
     */
    @Override
    public Collection<Integer> getYears()
    {
        return null;
    }

    /**
     * Obtains all songs from an album.
     *
     * @param album - The album to obtain
     * @return All songs from the requested album, or null if that album is not in the database.
     */
    @Override
    public Collection<Song> getSongsFromAlbum(Album album)
    {
        return null;
    }

    /**
     * Obtains all songs written by a given artist.
     *
     * @param artist - The artist to search for
     * @return A list of all songs from the specified artist, or null if that artist is not in the
     * database.
     */
    @Override
    public Collection<Song> getSongsFromArtist(String artist)
    {
        return null;
    }

    /**
     * Obtains an album by a specific name.
     *
     * @param name - The name to search for.
     * @return - The first album that matches the given name, or null if that album name is not in
     * the database.
     */
    @Override
    public Album getAlbumByName(String name)
    {
        return null;
    }

    /**
     * Obtains all albums that were written by a certain artist.
     *
     * @param artist - The artist to search for.
     * @return - The collection on matching albums.
     */
    @Override
    public Collection<Album> getAlbumsFromArtist(String artist)
    {
        return null;
    }

    /**
     * Obtains all albums that match a certain genre
     *
     * @param genre - The genre to search for.
     * @return - The collection on matching albums.
     */
    @Override
    public Collection<Album> getAlbumsFromGenre(String genre)
    {
        return null;
    }

    /**
     * Obtains all albums that were released a certain year.
     *
     * @param year - The year to search for.
     * @return - The collection on matching albums.
     */
    @Override
    public Collection<Album> getAlbumsFromYear(int year)
    {
        return null;
    }
}
