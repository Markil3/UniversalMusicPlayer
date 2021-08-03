/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import edu.regis.universeplayer.Player;
import edu.regis.universeplayer.browser.Browser;
import edu.regis.universeplayer.data.Queue;
import edu.regis.universeplayer.data.*;
import net.harawata.appdirs.AppDirsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * The Interface class serves as the primary GUI that the player interacts with.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class Interface extends JFrame implements SongDisplayListener, ComponentListener, WindowListener, PlaybackCommandListener, UpdateListener, FocusListener
{
    private static final Logger logger = LoggerFactory.getLogger(Interface.class);
    
    private static File dataDir;
    private static File configDir;
    
    /**
     * A reference to the panel containing links to different collection views.
     */
    private final Collections collectionTypes;
    /**
     * A reference to the panel showing the queue.
     */
    private final QueueList queueList;
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
            inter.setSize(700, 500);
            SongProvider.INSTANCE.addUpdateListener(inter);
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
        }
        catch (Throwable e)
        {
            logger.error("Could not open browser background", e);
            JOptionPane.showMessageDialog(inter != null && inter.isVisible() ? inter : null, e, "Error!", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Obtains the data storage directory for the application, creating it if
     * needed.
     * @return The data storage directory.
     */
    public static File getDataDir()
    {
        if (dataDir == null)
        {
            dataDir = new File(AppDirsFactory.getInstance().getUserDataDir("universalmusic", null, null, true));
            if (!dataDir.exists())
            {
                if (!dataDir.mkdir())
                {
                    logger.error("Could not create data directory {}", dataDir);
                }
            }
        }
        return dataDir;
    }
    
    /**
     * Obtains the configuration directory for the application, creating it if
     * needed.
     * @return The configuration directory.
     */
    public static File getConfigDir()
    {
        if (configDir == null)
        {
            configDir = new File(AppDirsFactory.getInstance().getUserConfigDir("universalmusic", null, null, true));
            if (!configDir.exists())
            {
                if (!configDir.mkdir())
                {
                    logger.error("Could not create configuration directory {}", configDir);
                }
            }
        }
        return configDir;
    }
    
    /**
     * Creates an interface
     */
    public Interface()
    {
        super();
        
        this.setTitle("Universal Music Player");
        this.getContentPane().setLayout(new BorderLayout());
        this.setFocusable(true);
        this.setFocusCycleRoot(true);
        
        this.getContentPane()
                .add(this.collectionTypes = new Collections(), BorderLayout.LINE_START);
        this.collectionTypes.addFocusListener(this);
        this.collectionTypes.addSongDisplayListener(this);
        
        this.controls = new PlayerControls();
        this.controls.addFocusListener(this);
        controls.addCommandListener(this);
        this.getContentPane().add(controls, BorderLayout.PAGE_END);
        
        this.queueList = new QueueList();
        this.queueList.addFocusListener(this);
        Queue.getInstance().addQueueChangeListener(this.queueList);
        Queue.getInstance().addSongChangeListener(this.queueList);
        this.getContentPane().add(queueList, BorderLayout.LINE_END);
        
        this.songList = new SongList();
        this.songList.addFocusListener(this);
        this.collectionList = new CollectionList();
        this.collectionList.addSongDisplayListener(this);
        
        this.centerView = new JScrollPane(this.songList);
        this.getContentPane().add(this.centerView, BorderLayout.CENTER);
        this.componentResized(null);
        
        this.addComponentListener(this);
        this.addWindowListener(this);
        this.addFocusListener(this);
    
        ((SortingFocusTraversalPolicy) this.getFocusTraversalPolicy()).setImplicitDownCycleTraversal(true);
        this.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Set.of(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_DOWN, 0), AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_RIGHT, 0)));
        this.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Set.of(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_UP, 0), AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_LEFT, 0)));
        this.setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, Set.of(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0)));
        this.setFocusTraversalKeys(KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS, Set.of(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK)));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
        this.updateSongs(SongProvider.INSTANCE.getSongs());
    }
    
    @Override
    public void updateSongs(Collection<? extends Song> songs)
    {
        this.songList.listAlbums(songs);
        this.centerView.setViewportView(this.songList);
//        this.songList.setPreferredSize(new Dimension(this.centerView.getViewport()
//                .getExtentSize().width, Integer.MAX_VALUE));
        this.songList.setPreferredSize(new Dimension(this.centerView.getViewport()
                .getExtentSize().width, this.songList
                .getMinimumSize().height));
        this.centerView.validate();
    }
    
    @Override
    public void updateCollections(CollectionType type, Collection<?> collections)
    {
        this.collectionList.listCollection(type, collections);
        this.centerView.setViewportView(this.collectionList);
//        this.collectionList.setPreferredSize(new Dimension(this.centerView.getViewport()
//                .getExtentSize().width, Integer.MAX_VALUE));
        this.collectionList.setPreferredSize(new Dimension(this.centerView.getViewport()
                .getExtentSize().width, this.collectionList
                .getMinimumSize().height));
        this.centerView.validate();
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
        Player player = null;
        if (this.currentPlayer >= 0 && this.currentPlayer < this.players.size())
        {
            player = this.players.get(this.currentPlayer);
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
        case NEXT -> Queue.getInstance().skipNext();
        case PREVIOUS -> Queue.getInstance().skipPrev();
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
    
    @Override
    public void focusGained(FocusEvent e)
    {
        Component parent;
        if (e.getOppositeComponent() == null)
        {
            collectionTypes.requestFocusInWindow();
            return;
        }
        parent = e.getOppositeComponent().getParent();
        if (e.getComponent() == this)
        {
            if (parent == collectionTypes)
            {
                centerView.getViewport().getView().requestFocusInWindow();
            }
            else if (parent == songList || parent == collectionList)
            {
                queueList.requestFocusInWindow();
            }
            else if (parent == queueList.songList || parent == queueList.header)
            {
                controls.requestFocusInWindow();
            }
            else if (parent == controls)
            {
                collectionTypes.requestFocusInWindow();
            }
            else
            {
                logger.warn("Unrecognized parent {}", parent);
                collectionTypes.requestFocusInWindow();
            }
        }
        /*
         * We are transfering backwards from an inner element
         */
        else
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
            if (index != -1)
            {
                parent = e.getComponent();
                if (parent == collectionTypes)
                {
                    centerView.getViewport().getView().requestFocusInWindow();
                }
                else if (parent == songList || parent == collectionList)
                {
                    queueList.requestFocusInWindow();
                }
                else if (parent == controls)
                {
                    controls.requestFocusInWindow();
                }
                else if (parent == queueList.songList || parent == queueList.header)
                {
                    collectionTypes.requestFocusInWindow();
                }
                else
                {
                    logger.warn("Unrecognized parent {}", parent);
                    collectionTypes.requestFocusInWindow();
                }
            }
        }
    }
    
    @Override
    public void focusLost(FocusEvent e)
    {
    }
}