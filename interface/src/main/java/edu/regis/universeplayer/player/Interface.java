/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import edu.regis.universeplayer.Player;
import edu.regis.universeplayer.browser.Browser;
import edu.regis.universeplayer.browser.BrowserPlayer;
import edu.regis.universeplayer.data.InternetSong;
import edu.regis.universeplayer.data.Queue;
import edu.regis.universeplayer.data.*;
import edu.regis.universeplayer.localPlayer.LocalPlayer;

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
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * The Interface class serves as the primary GUI that the player interacts with.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class Interface extends JFrame implements SongDisplayListener, ComponentListener, WindowListener, UpdateListener, FocusListener
{
    //    static {
//        Locale.setDefault(new Locale("es", "ES"));
//    }
    private static final Logger logger = LoggerFactory.getLogger(Interface.class);
    private static final ResourceBundle langs = ResourceBundle
            .getBundle("lang.interface", Locale.getDefault());

    private static Interface INSTANCE;

    private static File dataDir;
    private static File commDir;
    private static File configDir;

    public final ActionMap actions = new ActionMap();

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

    public static Interface getInstance()
    {
        return INSTANCE;
    }

    public static void main(String[] args)
    {
        Thread browserThread;
        Interface inter = null;
        Browser browser;
        BrowserPlayer browserPlayer;
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

            browser = Browser.createBrowser();
            browserThread = new Thread(browser);
            browserThread.start();

            Player.REGISTERED_PLAYERS.put(LocalSong.class, new LocalPlayer());

            try
            {
                inter.players.add(browserPlayer = new BrowserPlayer());
                logger.debug("Sending ping");
                browser.sendObject("ping");
                Runtime.getRuntime().addShutdownHook(new Thread(browserPlayer::close));
                Player.REGISTERED_PLAYERS.put(InternetSong.class, browserPlayer);

//                LinkedList<Future<Object>> pingRequests = new LinkedList<>();
//                for (int i = 0; i < 20; i++)
//                {
//                    logger.info("Sending ping");
//                    pingRequests.add(browser.sendObject("ping"));
//                }
//
//                LinkedList<Future<Object>> toRemove = new LinkedList<>();
//                while (pingRequests.size() > 0)
//                {
//                    for (Future<Object> future : pingRequests)
//                    {
//                        if (future.isDone())
//                        {
//                            logger.info("Receiving {}", future.get());
//                            toRemove.add(future);
//                        }
//                    }
//                    pingRequests.removeAll(toRemove);
//                    toRemove.clear();
//                }
            }
            catch (IOException e)
            {
                logger.error("Could not open browser background", e);
                JOptionPane.showMessageDialog(inter, e, langs
                        .getString("error.browser.launch"), JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (Throwable e)
        {
            logger.error("Could not open browser background", e);
            JOptionPane
                    .showMessageDialog(inter != null && inter.isVisible() ? inter : null, e, langs
                            .getString("error.generic"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Obtains the data storage directory for the application, creating it if
     * needed.
     *
     * @return The data storage directory.
     */
    public static File getDataDir()
    {
        if (dataDir == null)
        {
            dataDir = new File(AppDirsFactory.getInstance()
                                             .getUserDataDir("universalmusic", null, null, true));
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
     *
     * @return The configuration directory.
     */
    public static File getConfigDir()
    {
        if (configDir == null)
        {
            configDir = new File(AppDirsFactory.getInstance()
                                               .getUserConfigDir("universalmusic", null, null, true));
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
     * Obtains the directory for memory mapped files
     *
     * @return The communications storage directory.
     */
    public static File getCommDir()
    {
        if (commDir == null)
        {
            commDir = new File(AppDirsFactory.getInstance()
                                             .getSharedDir("universalmusic", null, null), "comm");
            if (!commDir.getParentFile().exists())
            {
                if (!commDir.getParentFile().mkdir())
                {
                    logger.error("Could not create shared directory {}", commDir.getParent());
                }
            }
            if (!commDir.exists())
            {
                if (!commDir.mkdir())
                {
                    logger.error("Could not create data directory {}", commDir);
                }
            }
        }
        return commDir;
    }

    /**
     * Creates an interface
     */
    public Interface()
    {
        super();
        INSTANCE = this;

        this.initActions();

        this.collectionTypes = new Collections();
        this.controls = new PlayerControls();
        this.queueList = new QueueList();
        this.songList = new SongList();
        this.collectionList = new CollectionList();
        this.centerView = new JScrollPane(this.songList);

        this.constructWindow();
        this.setFocusManager();

        this.updateSongs(SongProvider.INSTANCE.getSongs());
    }

    protected void initActions()
    {
        AbstractAction action;

        this.actions.put("refresh", action = new AbstractAction(langs.getString("actions.refresh"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {

                }
            }
        });
        action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);

        this.actions.put("addExternal", action = new AbstractAction(langs
                .getString("actions.addExternal"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {
                    InternetSongDialog dialog = new InternetSongDialog(Interface.this);
                    dialog.addWindowListener(new WindowAdapter()
                    {
                        /**
                         * Invoked when a window has been closed.
                         *
                         * @param e
                         */
                        @Override
                        public void windowClosed(WindowEvent e)
                        {
                        }
                    });
                    dialog.setVisible(true);
                }
            }
        });
        action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_E);

        this.actions.put("exit", action = new AbstractAction(langs.getString("actions.exit"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {
                    Interface.this
                            .dispatchEvent(new WindowEvent(Interface.this, WindowEvent.WINDOW_CLOSING));
                }
            }
        });
        action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_X);

        this.actions.put("logs", action = new AbstractAction(langs.getString("actions.logs"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {
                    LogWindow logs = new LogWindow();
                    logs.pack();
                    logs.setSize(600, 400);
                    logs.setVisible(true);
                }
            }
        });
        action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));

        this.actions.put("about", action = new AbstractAction(langs.getString("actions.about"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {

                }
            }
        });

        this.actions
                .put("view.all", action = new AbstractAction(langs.getString("actions.view.all"))
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        if (this.isEnabled())
                        {
                            collectionTypes
                                    .triggerSongDisplayListeners(new ArrayList<>(SongProvider.INSTANCE
                                            .getSongs()));
                        }
                    }
                });
        action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);

        this.actions.put("view.artists", action = new AbstractAction(langs
                .getString("actions.view.artists"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {
                    collectionTypes
                            .triggerCollectionDisplayListeners(CollectionType.albumArtist, SongProvider.INSTANCE
                                    .getAlbumArtists());
                }
            }
        });
        action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_T);

        this.actions.put("view.albums", action = new AbstractAction(langs
                .getString("actions.view.albums"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {
                    collectionTypes
                            .triggerCollectionDisplayListeners(CollectionType.album, SongProvider.INSTANCE
                                    .getAlbums());
                }
            }
        });
        action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);

        this.actions.put("view.genres", action = new AbstractAction(langs
                .getString("actions.view.genres"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {
                    collectionTypes
                            .triggerCollectionDisplayListeners(CollectionType.genre, SongProvider.INSTANCE
                                    .getGenres());
                }
            }
        });
        action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_G);

        this.actions.put("view.years", action = new AbstractAction(langs
                .getString("actions.view.years"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {
                    collectionTypes
                            .triggerCollectionDisplayListeners(CollectionType.year, SongProvider.INSTANCE
                                    .getYears());
                }
            }
        });
        action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_Y);

        this.actions.put("view.playlists", action = new AbstractAction(langs
                .getString("actions.view.playlists"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {

                }
            }
        });
        action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_L);

        this.actions.put("playback.clear", action = new AbstractAction(langs
                .getString("actions.playback.clear"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {
                    Queue.getInstance().clear();
                }
            }
        });
        action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);

        this.actions.put("playback.play", action = new AbstractAction(langs
                .getString("actions.playback.play"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {
                    controls.play();
                }
            }
        });

        this.actions.put("playback.pause", action = new AbstractAction(langs
                .getString("actions.playback.pause"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {
                    controls.pause();
                }
            }
        });

        this.actions.put("playback.toggle", action = new AbstractAction(langs
                .getString("actions.playback.toggle"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {
                    controls.togglePlayback();
                }
            }
        });

        this.actions.put("playback.skipPrev", action = new AbstractAction(langs
                .getString("actions.playback.skipPrev"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {
                    controls.previousSong();
                }
            }
        });

        this.actions.put("playback.skipNext", action = new AbstractAction(langs
                .getString("actions.playback.skipNext"))
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (this.isEnabled())
                {
                    controls.nextSong();
                }
            }
        });
    }

    protected void constructWindow()
    {
        JMenuBar toolbar;
        JMenu fileMenu, viewMenu, collectionsMenu, playbackMenu, helpMenu;

        this.setTitle(langs.getString("title"));
        this.getContentPane().setLayout(new BorderLayout());
        this.setFocusable(true);
        this.setFocusCycleRoot(true);

        this.getContentPane()
            .add(this.collectionTypes, BorderLayout.LINE_START);
        this.collectionTypes.addFocusListener(this);
        this.collectionTypes.addSongDisplayListener(this);

        this.controls.addFocusListener(this);
        this.getContentPane().add(controls, BorderLayout.PAGE_END);

        this.queueList.addFocusListener(this);
        Queue.getInstance().addQueueChangeListener(this.queueList);
        Queue.getInstance().addSongChangeListener(this.queueList);
        this.getContentPane().add(queueList, BorderLayout.LINE_END);

        this.songList.addFocusListener(this);
        this.collectionList.addSongDisplayListener(this);

        this.getContentPane().add(this.centerView, BorderLayout.CENTER);
        this.componentResized(null);

        toolbar = new JMenuBar();

        fileMenu = new JMenu(langs.getString("menu.file.title"));
        fileMenu.setMnemonic(KeyEvent.VK_F);
        toolbar.add(fileMenu);

        viewMenu = new JMenu(langs.getString("menu.view.title"));
        viewMenu.setMnemonic(KeyEvent.VK_V);
        toolbar.add(viewMenu);

        collectionsMenu = new JMenu(langs.getString("menu.view.collections"));
        collectionsMenu.setMnemonic(KeyEvent.VK_C);
        viewMenu.add(collectionsMenu);

        playbackMenu = new JMenu(langs.getString("menu.playback.title"));
        playbackMenu.setMnemonic(KeyEvent.VK_P);
        toolbar.add(playbackMenu);

        helpMenu = new JMenu(langs.getString("menu.help.title"));
        helpMenu.setMnemonic(KeyEvent.VK_H);
        toolbar.add(helpMenu);

        fileMenu.add(new JMenuItem(actions.get("refresh")));

        fileMenu.add(actions.get("addExternal"));

        fileMenu.add(new JMenuItem(actions.get("exit")));

        collectionsMenu.add(new JMenuItem(actions.get("view.all")));
        collectionsMenu.add(new JMenuItem(actions.get("view.artists")));
        collectionsMenu.add(new JMenuItem(actions.get("view.albums")));
        collectionsMenu.add(new JMenuItem(actions.get("view.genres")));
        collectionsMenu.add(new JMenuItem(actions.get("view.years")));
        collectionsMenu.add(new JMenuItem(actions.get("view.playlists")));

        playbackMenu.add(new JMenuItem(actions.get("playback.clear")));
        playbackMenu.addSeparator();
        playbackMenu.add(new JMenuItem(actions.get("playback.toggle")));
        playbackMenu.add(new JMenuItem(actions.get("playback.skipPrev")));
        playbackMenu.add(new JMenuItem(actions.get("playback.skipNext")));

        helpMenu.add(new JMenuItem(actions.get("logs")));

        helpMenu.add(new JMenuItem(actions.get("about")));

        this.setJMenuBar(toolbar);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.addComponentListener(this);
        this.addWindowListener(this);
        this.addFocusListener(this);
    }

    protected void setFocusManager()
    {
        ((SortingFocusTraversalPolicy) this.getFocusTraversalPolicy())
                .setImplicitDownCycleTraversal(true);
        this.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Set
                .of(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_DOWN, 0), AWTKeyStroke
                        .getAWTKeyStroke(KeyEvent.VK_RIGHT, 0)));
        this.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Set
                .of(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_UP, 0), AWTKeyStroke
                        .getAWTKeyStroke(KeyEvent.VK_LEFT, 0)));
        this.setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, Set
                .of(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0)));
        this.setFocusTraversalKeys(KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS, Set
                .of(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK)));
    }

    @Override
    public void updateSongs(Collection<? extends Song> songs)
    {
        this.songList.listAlbums(songs);
        this.centerView.setViewportView(this.songList);
        this.songList.revalidate();
        this.centerView.revalidate();
    }

    @Override
    public void updateCollections(CollectionType type, Collection<?> collections)
    {
        this.collectionList.listCollection(type, collections);
        this.centerView.setViewportView(this.collectionList);
        this.collectionList.revalidate();
        this.centerView.revalidate();
    }

    @Override
    public void componentResized(ComponentEvent event)
    {
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