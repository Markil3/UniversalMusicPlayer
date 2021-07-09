/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universe_player.player;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import edu.regis.universe_player.data.CollectionType;
import edu.regis.universe_player.data.Song;

/**
 * The Interface class serves as the primary GUI that the player interacts with.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class Interface extends JFrame implements SongDisplayListener, ComponentListener
{
    /**
     * A reference to the panel containing links to different collection views.
     */
    private Collections collectionTypes;
    /**
     * A reference to the central view showing a list of songs.
     */
    private SongList songList;
    /**
     * A reference to the central view showing a list of collections.
     */
    private CollectionList collectionList;
    /**
     * A reference to the central view scroll pane.
     */
    private JScrollPane centerView;

    public static void main(String[] args)
    {
        Interface inter = new Interface();
        inter.pack();
        inter.setVisible(true);
    }

    /**
     * Creates an interface
     */
    public Interface()
    {
        super();
        this.setTitle("Universal Music Player");
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane()
            .add(this.collectionTypes = new Collections(), BorderLayout.LINE_START);
        this.collectionTypes.addSongDisplayListener(this);
        this.getContentPane().add(new PlayerControls(), BorderLayout.PAGE_END);

        this.songList = new SongList();
        this.collectionList = new CollectionList();
        this.collectionList.addSongDisplayListener(this);

        this.centerView = new JScrollPane(this.songList);
        this.getContentPane().add(this.centerView, BorderLayout.CENTER);
        this.componentResized(null);

        this.addComponentListener(this);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    @Override
    public void updateSongs(Collection<Song> songs)
    {
        this.songList.listAlbums(songs);
        this.centerView.setViewportView(this.songList);
        this.songList.setPreferredSize(new Dimension(this.centerView.getViewport()
                                                                    .getExtentSize().width, Integer.MAX_VALUE));
        this.songList.setPreferredSize(new Dimension(this.centerView.getViewport()
                                                                    .getExtentSize().width, this.songList
                .getMinimumSize().height));
    }

    @Override
    public void updateCollections(CollectionType type, Collection<?> collections)
    {
        this.collectionList.listCollection(type, collections);
        this.centerView.setViewportView(this.collectionList);
        this.collectionList.setPreferredSize(new Dimension(this.centerView.getViewport()
                                                                          .getExtentSize().width, Integer.MAX_VALUE));
        this.collectionList.setPreferredSize(new Dimension(this.centerView.getViewport()
                                                                          .getExtentSize().width, this.collectionList
                .getMinimumSize().height));
    }

    @Override
    public void componentResized(ComponentEvent event)
    {
        this.songList.setPreferredSize(new Dimension(this.centerView.getViewport()
                                                                    .getExtentSize().width, Integer.MAX_VALUE));
        this.songList.setPreferredSize(new Dimension(this.centerView.getViewport()
                                                                    .getExtentSize().width, this.songList
                .getMinimumSize().height));
        this.collectionList.setPreferredSize(new Dimension(this.centerView.getViewport()
                                                                          .getExtentSize().width, Integer.MAX_VALUE));
        this.collectionList.setPreferredSize(new Dimension(this.centerView.getViewport()
                                                                          .getExtentSize().width, this.collectionList
                .getMinimumSize().height));
    }

    @Override
    public void componentMoved(ComponentEvent event)
    {

    }

    @Override
    public void componentShown(ComponentEvent event)
    {

    }

    @Override
    public void componentHidden(ComponentEvent event)
    {

    }
}