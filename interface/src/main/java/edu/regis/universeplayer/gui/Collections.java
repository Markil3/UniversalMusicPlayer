/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.*;

import edu.regis.universeplayer.data.CollectionType;
import edu.regis.universeplayer.data.Song;

/**
 * This panel links to various song collections the player has set up.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class Collections extends JPanel
{
    private static final ResourceBundle langs = ResourceBundle.getBundle("lang.interface", Locale.getDefault());

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
        
        this.add(defaultLabel = label = this.createButton(Interface.getInstance().actions.get("view.all")));
        this.add(label = this.createButton(Interface.getInstance().actions.get("view.artists")));
        this.add(label = this.createButton(Interface.getInstance().actions.get("view.albums")));
        this.add(label = this.createButton(Interface.getInstance().actions.get("view.genres")));
        this.add(label = this.createButton(Interface.getInstance().actions.get("view.years")));
        this.add(new JLabel("\u23AF".repeat(6)));
        this.add(label = this.createButton(Interface.getInstance().actions.get("view.playlists")));
        this.add(new JLabel("\u23AF".repeat(6)));
        this.add(label = this.createButton(Interface.getInstance().actions.get("addExternal")));
        
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
    
    private void addNewSong()
    {
        Container parent = this.getParent();
        while (!(parent instanceof JFrame) && parent != null)
        {
            parent = parent.getParent();
        }
        new InternetSongDialog((JFrame) parent).setVisible(true);
    }
    
    private JButton createButton(String text)
    {
        return setButton(new JButton(text));
    }

    private JButton createButton(Action action)
    {
        return setButton(new JButton(action));
    }

    private JButton setButton(JButton label)
    {
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
