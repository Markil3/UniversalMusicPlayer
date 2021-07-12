/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universe_player.player;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import edu.regis.universe_player.browser.Browser;
import edu.regis.universe_player.browser.MessageManager;
import edu.regis.universe_player.data.CollectionType;
import edu.regis.universe_player.data.Song;

/**
 * The Interface class serves as the primary GUI that the player interacts with.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class Interface extends JFrame implements SongDisplayListener, ComponentListener, WindowListener, PlaybackCommandListener
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

    /**
     * A link to the browser.
     */
    private MessageManager browser;

    public static void main(String[] args)
    {
        try
        {
            Browser.launchBrowser();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e, "Could not open browser background", JOptionPane.ERROR_MESSAGE);
        }
        Interface inter = new Interface();
        try
        {
            inter.browser = new MessageManager();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e, "Could not open browser communication", JOptionPane.ERROR_MESSAGE);
        }
        inter.pack();
        inter.setVisible(true);
    }

    /**
     * Creates an interface
     */
    public Interface()
    {
        super();
        PlayerControls controls;

        this.setTitle("Universal Music Player");
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane()
            .add(this.collectionTypes = new Collections(), BorderLayout.LINE_START);
        this.collectionTypes.addSongDisplayListener(this);
        controls = new PlayerControls();
        controls.addCommandListener(this);
        this.getContentPane().add(controls, BorderLayout.PAGE_END);

        this.songList = new SongList();
        this.collectionList = new CollectionList();
        this.collectionList.addSongDisplayListener(this);

        this.centerView = new JScrollPane(this.songList);
        this.getContentPane().add(this.centerView, BorderLayout.CENTER);
        this.componentResized(null);

        this.addComponentListener(this);
        this.addWindowListener(this);

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

    @Override
    public void windowOpened(WindowEvent windowEvent)
    {

    }

    @Override
    public void windowClosing(WindowEvent windowEvent)
    {
        Browser.closeBrowser();
        if (this.browser != null)
        {
            try
            {
                this.browser.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void windowClosed(WindowEvent windowEvent)
    {
    }

    @Override
    public void windowIconified(WindowEvent windowEvent)
    {

    }

    @Override
    public void windowDeiconified(WindowEvent windowEvent)
    {

    }

    @Override
    public void windowActivated(WindowEvent windowEvent)
    {

    }

    @Override
    public void windowDeactivated(WindowEvent windowEvent)
    {

    }

    /**
     * Called when a playback command is issued.
     *
     * @param command - The command issued.
     * @param data    - Additional data relevent to the command.
     */
    @Override
    public void onCommand(PlaybackCommand command, Object data)
    {
        Object message = null;
        if (this.browser != null)
        {
            synchronized (this.browser)
            {
                try
                {
                    this.browser.ping();
                    message = this.browser.getMessage();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, e, "Could not send message to browser", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        if (message != null)
        {
            if ("ping".equals(message))
            {
                JOptionPane.showMessageDialog(this, "Ping received!");
            }
        }
    }
}