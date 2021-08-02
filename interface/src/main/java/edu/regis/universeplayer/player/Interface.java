/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import edu.regis.universeplayer.Player;
import edu.regis.universeplayer.browser.Browser;
import edu.regis.universeplayer.data.CollectionType;
import edu.regis.universeplayer.data.Song;
import edu.regis.universeplayer.data.SongProvider;
import edu.regis.universeplayer.data.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Future;

/**
 * The Interface class serves as the primary GUI that the player interacts with.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class Interface extends JFrame implements SongDisplayListener, ComponentListener, WindowListener, PlaybackCommandListener, UpdateListener
{
    private static final Logger logger = LoggerFactory.getLogger(Interface.class);
    
    /**
     * A reference to the panel containing links to different collection views.
     */
    private final Collections collectionTypes;
    /**
     * A reference to the central view showing a list of songs.
     */
    private final SongList songList;
    /**
     * A reference to the central view showing a list of collections.
     */
    private final CollectionList collectionList;
    /**
     * A reference to the central view scroll pane.
     */
    private final JScrollPane centerView;
    /**
     * A reference to the player controls pane.
     */
    private final PlayerControls controls;
    
    /**
     * A link to the browser.
     */
    private final ArrayList<Player<?>> players = new ArrayList<>();
    private int currentPlayer = -1;
    
    public static void main(String[] args)
    {
        Thread browserThread;
        Interface inter = null;
        Browser browser;
        try
        {
            /*
             * Add this just in case of a crash or something. It won't work if the
             * program is forcibly terminated by the OS, but it could be helpful
             * otherwise.
             */
            logger.info("Starting application");
            inter = new Interface();
            inter.pack();
            inter.setVisible(true);
            
            try
            {
                inter.players.add(browser = Browser.createBrowser());
                browserThread = new Thread(browser);
                browserThread.start();
                Runtime.getRuntime().addShutdownHook(new Thread(browser::close));
                
                LinkedList<Future<Object>> pingRequests = new LinkedList<>();
                for (int i = 0; i < 20; i++)
                {
                    logger.info("Sending ping");
                    pingRequests.add(browser.sendObject("ping"));
                }
                
                LinkedList<Future<Object>> toRemove = new LinkedList<>();
                while (pingRequests.size() > 0)
                {
                    for (Future<Object> future : pingRequests)
                    {
                        if (future.isDone())
                        {
                            logger.info("Receiving {}", future.get());
                            toRemove.add(future);
                        }
                    }
                    pingRequests.removeAll(toRemove);
                    toRemove.clear();
                }
            }
            catch (IOException e)
            {
                logger.error("Could not open browser background", e);
                JOptionPane.showMessageDialog(inter, e, "Could not open browser background", JOptionPane.ERROR_MESSAGE);
            }
            
            SongProvider.INSTANCE.addUpdateListener(inter);
        }
        catch (Throwable e)
        {
            logger.error("Could not open browser background", e);
            JOptionPane.showMessageDialog(inter != null && inter.isVisible() ? inter : null, e, "Error!", JOptionPane.ERROR_MESSAGE);
        }
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
        this.controls = new PlayerControls();
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
    public void updateSongs(Collection<? extends Song> songs)
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
        logger.info("Interface opened");
    }
    
    @Override
    public void windowClosing(WindowEvent windowEvent)
    {
        this.players.forEach(Player::close);
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
        Player player;
        if (this.currentPlayer >= 0 && this.currentPlayer < this.players.size())
        {
            player = this.players.get(this.currentPlayer);
        }
        else
        {
            throw new NullPointerException("No player available");
        }
        switch (command)
        {
        case PLAY -> {
            if (data instanceof Song)
            {
                player.loadSong((Song) data);
            }
        }
        case PAUSE -> {
        }
        case NEXT -> {
        }
        case PREVIOUS -> {
        }
        case SEEK -> {
        }
        }
    }
    
    @Override
    public void onUpdate(int updated, int totalUpdate, String updating)
    {
        this.controls.setUpdateProgress(updated, totalUpdate, updating);
        if (updated == totalUpdate || totalUpdate == 0)
        {
            logger.debug("Resetting the song provider.");
            /*
             * TODO - Add some way to get back to the current view, just updated
             */
            this.updateSongs(SongProvider.INSTANCE.getSongs());
        }
    }
}