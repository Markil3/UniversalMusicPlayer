/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A song provider that serves as a central point for any and all song providers, caching the results in memory for quick and easy access.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class CompiledSongProvider implements SongProvider
{
    /**
     * A set of all providers we pull from
     */
    private HashMap<SongProvider, Set<Song>> providers = new HashMap<>();

    /**
     * A cache of all albums used.
     */
    private HashMap<Album, Set<Song>> cachedAlbums = new HashMap<>();

    /**
     * A cache of all album names.
     */
    private HashMap<String, Album> cachedAlbumNames = new HashMap<>();

    /**
     * A cache of all songs used.
     */
    private HashSet<Song> cachedSongs = new HashSet<>();

    /**
     * A cache of all song artists used.
     */
    private HashMap<String, Set<Song>> cachedArtists = new HashMap<>();

    /**
     * A cache of all album artists used.
     */
    private HashMap<String, Set<Album>> cachedAlbumArtists = new HashMap<>();

    /**
     * A cache of all genres used.
     */
    private HashMap<String, Set<Album>> cachedGenres = new HashMap<>();

    /**
     * A cache of all years used.
     */
    private HashMap<Integer, Set<Album>> cachedYears = new HashMap<>();

    /**
     * Creates a new CompiledSongProvider containing a set of existing providers.
     *
     * @param providers - Providers to add.
     */
    public CompiledSongProvider(SongProvider... providers)
    {
        for (SongProvider provider : providers)
        {
            this.addProvider(provider);
        }
    }

    /**
     * Adds a provider to the list
     *
     * @param provider - The provider to add.
     */
    public void addProvider(SongProvider provider)
    {
        if (!this.providers.containsKey(provider))
        {
            this.providers.put(provider, new HashSet<>(provider.getSongs()));
            this.addToCache(provider);
        }
    }

    /**
     * Caches all the songs contained in a provider.
     *
     * @param provider - The provider to cache.
     */
    private void addToCache(SongProvider provider)
    {
        for (Song song : provider.getSongs())
        {
            if (this.cachedSongs.add(song))
            {
                this.cachedAlbumNames.put(song.album.name, song.album);
                if (!this.cachedAlbums.containsKey(song.album))
                {
                    this.cachedAlbums.put(song.album, new HashSet<>());
                }
                this.cachedAlbums.get(song.album).add(song);
                for (String artist : song.artists)
                {
                    if (!this.cachedArtists.containsKey(artist))
                    {
                        this.cachedArtists.put(artist, new HashSet<>());
                    }
                    this.cachedArtists.get(artist).add(song);
                }
                for (String artist : song.album.artists)
                {
                    if (!this.cachedAlbumArtists.containsKey(artist))
                    {
                        this.cachedAlbumArtists.put(artist, new HashSet<>());
                    }
                    this.cachedAlbumArtists.get(artist).add(song.album);
                }
                for (String genre : song.album.genres)
                {
                    if (!this.cachedGenres.containsKey(genre))
                    {
                        this.cachedGenres.put(genre, new HashSet<>());
                    }
                    this.cachedGenres.get(genre).add(song.album);
                }
                if (!this.cachedYears.containsKey(song.album.year))
                {
                    this.cachedYears.put(song.album.year, new HashSet<>());
                }
                this.cachedYears.get(song.album.year).add(song.album);
            }
            else
            {
                /*
                 * If we couldn't add it, then another provider has provided that song already. We
                 * should remove it from our collection just to ensure that there is no confusion.
                 */
                this.providers.get(provider).remove(song);
            }
        }
    }

    /**
     * Removes a provider from the compilation.
     *
     * @param provider - The provider to remove.
     */
    public void removeProvider(SongProvider provider)
    {
        this.removeFromCache(provider);
    }

    /**
     * Removes all songs from a provider from a cache.
     *
     * @param provider - The provider to move out.
     */
    private void removeFromCache(SongProvider provider)
    {
        Set<Song> songs = this.providers.remove(provider);
        if (songs != null)
        {
            for (Song song : songs)
            {
                this.cachedSongs.remove(song);
                this.cachedAlbums.get(song.album).remove(song);
                /*
                 * Remove empty albums
                 */
                if (this.cachedAlbums.get(song.album).isEmpty())
                {
                    this.cachedAlbums.remove(song.album);
                    this.cachedAlbumNames.remove(song.album.name);
                }
                for (String artist : song.artists)
                {
                    this.cachedArtists.get(artist).remove(song);
                    if (this.cachedArtists.get(artist).isEmpty())
                    {
                        this.cachedArtists.remove(artist);
                    }
                }
                for (String artist : song.album.artists)
                {
                    this.cachedAlbumArtists.get(artist).remove(song.album);
                    if (this.cachedAlbumArtists.get(artist).isEmpty())
                    {
                        this.cachedAlbumArtists.remove(artist);
                    }
                }
                for (String genre : song.album.genres)
                {
                    this.cachedGenres.get(genre).remove(song.album);
                    if (this.cachedGenres.get(genre).isEmpty())
                    {
                        this.cachedGenres.remove(genre);
                    }
                }
                this.cachedYears.get(song.album.year).remove(song.album);
                if (this.cachedYears.get(song.album.year).isEmpty())
                {
                    this.cachedYears.remove(song.album.year);
                }
            }
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
        return this.cachedAlbums.keySet();
    }

    /**
     * Obtains all songs within the collection.
     *
     * @return A list of songs.
     */
    @Override
    public Collection<Song> getSongs()
    {
        return this.cachedSongs;
    }

    /**
     * Obtains a list of all artists.
     *
     * @return All artists.
     */
    @Override
    public Collection<String> getArtists()
    {
        return this.cachedArtists.keySet();
    }

    /**
     * Obtains a list of all album artists.
     *
     * @return All album artists.
     */
    @Override
    public Collection<String> getAlbumArtists()
    {
        return this.cachedAlbumArtists.keySet();
    }

    /**
     * Obtains a list of all genres.
     *
     * @return All genres.
     */
    @Override
    public Collection<String> getGenres()
    {
        return this.cachedGenres.keySet();
    }

    /**
     * Obtains a list of all years that have albums.
     *
     * @return All years.
     */
    @Override
    public Collection<Integer> getYears()
    {
        return this.cachedYears.keySet();
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
        return this.cachedAlbums.get(album);
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
        return this.cachedArtists.get(artist);
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
        return this.cachedAlbumNames.get(name);
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
        return this.cachedAlbumArtists.get(artist);
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
        return this.cachedGenres.get(genre);
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
        return this.cachedYears.get(year);
    }
}
