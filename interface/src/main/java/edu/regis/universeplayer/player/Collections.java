/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.regis.universeplayer.ClickListener;
import edu.regis.universeplayer.data.CollectionType;
import edu.regis.universeplayer.data.Song;
import edu.regis.universeplayer.data.SongProvider;

/**
 * This panel links to various song collections the player has set up.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class Collections extends JPanel
{
    /**
     * A list of all things interested in knowing when we click a collection.
     */
    private LinkedList<SongDisplayListener> listeners = new LinkedList<>();
    
    /**
     * Creates a collections list view.
     */
    public Collections()
    {
        JLabel label;
        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        this.setLayout(layout);
        
        this.add(label = new JLabel("All Songs"));
        label.setForeground(Color.BLUE);
        label.addMouseListener((ClickListener) mouseEvent -> this
                .triggerSongDisplayListeners(new ArrayList<>(SongProvider.INSTANCE.getSongs())));
        this.add(label = new JLabel("Artists"));
        label.setForeground(Color.BLUE);
        label.addMouseListener((ClickListener) mouseEvent -> this
                .triggerCollectionDisplayListeners(CollectionType.albumArtist, SongProvider.INSTANCE
                        .getAlbumArtists()));
        this.add(label = new JLabel("Albums"));
        label.setForeground(Color.BLUE);
        label.addMouseListener((ClickListener) mouseEvent -> this
                .triggerCollectionDisplayListeners(CollectionType.album, SongProvider.INSTANCE
                        .getAlbums()));
        this.add(label = new JLabel("Genres"));
        label.setForeground(Color.BLUE);
        label.addMouseListener((ClickListener) mouseEvent -> this
                .triggerCollectionDisplayListeners(CollectionType.genre, SongProvider.INSTANCE
                        .getGenres()));
        this.add(label = new JLabel("Years"));
        label.setForeground(Color.BLUE);
        label.addMouseListener((ClickListener) mouseEvent -> this
                .triggerCollectionDisplayListeners(CollectionType.year, SongProvider.INSTANCE
                        .getYears()));
        this.add(new JLabel("\u23AF\u23AF\u23AF\u23AF\u23AF\u23AF"));
        this.add(label = new JLabel("Playlists"));
        label.setForeground(Color.BLUE);
    }
    
    /**
     * Adds a listener for when the displayed songs should change.
     *
     * @param listener - The listener to add.
     */
    public void addSongDisplayListener(SongDisplayListener listener)
    {
        this.listeners.add(listener);
    }
    
    /**
     * Adds a listener for when the displayed songs should change.
     *
     * @param listener - The listener to add.
     */
    public void removeSongDisplayListener(SongDisplayListener listener)
    {
        this.listeners.remove(listener);
    }
    
    /**
     * Triggers all the song display listeners.
     */
    protected void triggerSongDisplayListeners(Collection<Song> songs)
    {
        for (SongDisplayListener listener : this.listeners)
        {
            listener.updateSongs(songs);
        }
    }
    
    /**
     * Triggers all the song display listeners.
     */
    protected void triggerCollectionDisplayListeners(CollectionType type, Collection<?> collection)
    {
        for (SongDisplayListener listener : this.listeners)
        {
            listener.updateCollections(type, collection);
        }
    }
}
