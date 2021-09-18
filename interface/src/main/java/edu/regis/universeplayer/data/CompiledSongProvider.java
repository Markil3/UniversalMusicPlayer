/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A song provider that serves as a central point for any and all song
 * providers, caching the results in memory for quick and easy access.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class CompiledSongProvider implements SongProvider<Song>, UpdateListener
{
    private final LinkedList<UpdateListener> listeners = new LinkedList<>();

    /**
     * A set of all providers we pull from
     */
    private final HashSet<SongProvider<? extends Song>> providers =
            new HashSet<>();
    private AlbumProvider albums;

    /**
     * Creates a new CompiledSongProvider containing a set of existing
     * providers.
     *
     * @param providers - Providers to add.
     */
    public CompiledSongProvider(SongProvider<?
            >... providers)
    {
        for (SongProvider<?> provider : providers)
        {
            this.addProvider(provider);
        }
    }

    /**
     * Adds a provider to the list
     *
     * @param provider - The provider to add.
     */
    public <T extends Song> void addProvider(SongProvider<T> provider)
    {
        UpdateListener listener;
        if (this.providers.add(provider))
        {
            if (this.albums == null)
            {
                this.albums = provider.getAlbumProvider();
            }
            provider.addUpdateListener(this);
            triggerUpdateListeners();
        }
    }

    /**
     * Removes a provider from the compilation.
     *
     * @param provider - The provider to remove.
     */
    public void removeProvider(SongProvider<?> provider)
    {
        provider.removeUpdateListener(this);
        this.providers.remove(provider);
    }

    @Override
    public AlbumProvider getAlbumProvider()
    {
        return this.albums;
    }

    @Override
    public void joinUpdate() throws InterruptedException
    {
        for (SongProvider provider: this.providers)
        {
            provider.joinUpdate();
        }
    }

    /**
     * Obtains the collection of items.
     *
     * @return A collection of items parsed from the database.
     */
    @Override
    public Set<Song> getCollection()
    {
        return this.providers.stream().map(SongProvider::getCollection)
                             .flatMap(Collection::stream)
                             .collect(Collectors.toSet());
    }

    /**
     * Obtains all songs within the collection.
     *
     * @return A list of songs.
     */
    @Override
    public Collection<Song> getSongs()
    {
        return this.getCollection();
    }

    /**
     * Obtains a list of all artists.
     *
     * @return All artists.
     */
    @Override
    public Collection<String> getArtists()
    {
        return this.providers.stream().map(SongProvider::getArtists)
                             .flatMap(Collection::stream)
                             .collect(Collectors.toSet());
    }

    /**
     * Obtains all songs from an album.
     *
     * @param album - The album to obtain
     * @return All songs from the requested album, or null if that album is not
     * in the database.
     */
    @Override
    public Collection<Song> getSongsFromAlbum(Album album)
    {
        return this.providers.stream()
                             .map(p -> p.getSongsFromAlbum(album))
                             .flatMap(Collection::stream)
                             .collect(Collectors.toSet());
    }

    /**
     * Obtains all songs written by a given artist.
     *
     * @param artist - The artist to search for
     * @return A list of all songs from the specified artist, or null if that
     * artist is not in the database.
     */
    @Override
    public Collection<Song> getSongsFromArtist(String artist)
    {
        return this.providers.stream()
                             .map(p -> p.getSongsFromArtist(artist))
                             .flatMap(Collection::stream)
                             .collect(Collectors.toSet());
    }

    @Override
    public int getUpdateProgress()
    {
        int totalUpdate = 0;
        for (SongProvider<?> provider : this.providers)
        {
            totalUpdate += provider.getUpdateProgress();
        }
        return totalUpdate;
    }

    @Override
    public int getTotalUpdates()
    {
        int totalUpdate = 0;
        for (SongProvider<?> provider : this.providers)
        {
            if (provider.getTotalUpdates() == -1)
            {
                return -1;
            }
            else
            {
                totalUpdate += provider.getTotalUpdates();
            }
        }
        return totalUpdate;
    }

    @Override
    public String getUpdateText()
    {
        for (SongProvider<?> provider : this.providers)
        {
            if (provider.getUpdateText() != null)
            {
                return provider.getUpdateText();
            }
        }
        return null;
    }

    @Override
    public void addUpdateListener(UpdateListener listener)
    {
        this.listeners.add(listener);
    }

    @Override
    public void removeUpdateListener(UpdateListener listener)
    {
        this.listeners.remove(listener);
    }

    /**
     * Triggers all update listeners.
     */
    protected void triggerUpdateListeners()
    {
        for (UpdateListener listener : this.listeners)
        {
            listener.onUpdate(this, this.getUpdateProgress(),
                    this.getTotalUpdates(), this.getUpdateText());
        }
    }

    /**
     * Called when the update status of the player has changed.
     *
     * @param provider    - The provider that triggered the listener.
     * @param updated     - The number of songs updated.
     * @param totalUpdate - The total number of songs to update, or -1 if we are
     *                    still determining that.
     * @param updating    - The text to display on update bars.
     */
    @Override
    public <T> void onUpdate(DataProvider<T> provider, int updated, int totalUpdate, String updating)
    {
        this.triggerUpdateListeners();
    }
}
