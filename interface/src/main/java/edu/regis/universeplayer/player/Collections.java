/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.LabelUI;

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
        JButton defaultLabel;
        JButton label;
        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        this.setLayout(layout);
        this.setFocusable(true);
        this.setFocusCycleRoot(true);
        
        this.add(defaultLabel = label = this.createButton("All Songs"));
        label.setMnemonic('A');
        label.addActionListener(mouseEvent -> this
                .triggerSongDisplayListeners(new ArrayList<>(SongProvider.INSTANCE.getSongs())));
        this.add(label = this.createButton("Artists"));
        label.setMnemonic('T');
        label.addActionListener(mouseEvent -> this
                .triggerCollectionDisplayListeners(CollectionType.albumArtist, SongProvider.INSTANCE
                        .getAlbumArtists()));
        this.add(label = this.createButton("Albums"));
        label.setMnemonic('B');
        label.addActionListener(mouseEvent -> this
                .triggerCollectionDisplayListeners(CollectionType.album, SongProvider.INSTANCE
                        .getAlbums()));
        this.add(label = this.createButton("Genres"));
        label.setMnemonic('G');
        label.addActionListener(mouseEvent -> this
                .triggerCollectionDisplayListeners(CollectionType.genre, SongProvider.INSTANCE
                        .getGenres()));
        this.add(label = this.createButton("Years"));
        label.setMnemonic('Y');
        label.addActionListener(mouseEvent -> this
                .triggerCollectionDisplayListeners(CollectionType.year, SongProvider.INSTANCE
                        .getYears()));
        this.add(new JLabel("\u23AF".repeat(6)));
        this.add(label = this.createButton("Playlists"));
        label.setMnemonic('P');
        
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                defaultLabel.requestFocusInWindow();
            }
        });
        addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                int index = -1;
                Component[] children = ((Container) e.getComponent()).getComponents();
                for (int i = 0, l = children.length; index == -1 && i < l; i++)
                {
                    if (children[i] == e.getOppositeComponent())
                    {
                        index = i;
                    }
                }
                /*
                 * Only auto-switch focus if the previous component was not from
                 * within.
                 */
                if (index == -1)
                {
                    defaultLabel.requestFocusInWindow();
                }
            }
        });
    }
    
    private JButton createButton(String text)
    {
        JButton label = new JButton(text);
        label.setFocusPainted(true);
        label.setMargin(new Insets(0, 0, 0, 0));
        label.setContentAreaFilled(false);
        label.setBorderPainted(false);
        label.setOpaque(false);
        label.setForeground(Color.BLUE);
        return label;
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
