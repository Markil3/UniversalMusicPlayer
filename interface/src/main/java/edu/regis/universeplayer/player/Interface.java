/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

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

import edu.regis.universeplayer.browser.Browser;
import edu.regis.universeplayer.browser.MessageManager;
import edu.regis.universeplayer.data.CollectionType;
import edu.regis.universeplayer.data.Song;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Interface class serves as the primary GUI that the player interacts with.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class Interface extends JFrame implements SongDisplayListener, ComponentListener, WindowListener, PlaybackCommandListener
{
    private static Logger logger = LoggerFactory.getLogger(Interface.class);
    
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
        /*
         * Add this just in case of a crash or something. It won't work if the
         * program is forcible terminated by the OS, but it could be helpful
         * otherwise.
         */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Browser.closeBrowser();
        }));
        logger.info("Starting application");
        Interface inter = new Interface();
        try
        {
            inter.browser = new MessageManager();
        }
        catch (IOException e)
        {
            logger.error("Could not open browser communication", e);
            JOptionPane.showMessageDialog(null, e, "Could not open browser communication", JOptionPane.ERROR_MESSAGE);
        }
        inter.pack();
        inter.setVisible(true);
        
        /*
         * Launch the browser in the background.
         */
        try
        {
            Browser.launchBrowser();
        }
        catch (IOException e)
        {
            logger.error("Could not open browser background", e);
            JOptionPane.showMessageDialog(inter, e, "Could not open browser background", JOptionPane.ERROR_MESSAGE);
        }
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
        logger.info("Interface opened");
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
                logger.error("Could not close browser", e);
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
                    logger.error("Could not send message to browser", e);
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