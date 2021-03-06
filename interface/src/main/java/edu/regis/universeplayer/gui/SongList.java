/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.gui;

import com.wordpress.tips4java.ScrollablePanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.regis.universeplayer.ClickListener;
import edu.regis.universeplayer.PlayerEnvironment;
import edu.regis.universeplayer.data.Queue;
import edu.regis.universeplayer.data.*;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * This panel will list all the songs that are to be currently displayed.
 */
public class SongList extends ScrollablePanel
{
    private static final Logger logger = LoggerFactory
            .getLogger(SongList.class);
    private static final ResourceBundle langs = ResourceBundle
            .getBundle("lang.interface", Locale.getDefault());

    private Map<Album, List<Song>> currentAlbums;
    private Map<JComponent, Song> labelMap = new HashMap<>();
    private Map<AlbumInfo, Album> artMap = new HashMap<>();

    public SongList()
    {
        super();

        GridBagLayout layout = new GridBagLayout();
        this.setFocusTraversalPolicyProvider(true);
        this.setLayout(layout);

        SongProvider<?> provider = PlayerEnvironment.getSongs();
        this.listAlbums(provider.getSongs());

        this.setScrollableWidth(ScrollableSizeHint.FIT);
        this.setScrollableHeight(ScrollableSizeHint.STRETCH);
        this.setFocusTraversalPolicy(new SongListPolicy());
        this.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                int index = -1;
                Component[] children = ((Container) e.getComponent())
                        .getComponents();
                for (int i = 0, l = children.length; index == -1 && i < l; i++)
                {
                    if (children[i] == e.getOppositeComponent())
                    {
                        index = i;
                    }
                }
                if (index == -1)
                {
                    artMap.keySet().stream().findFirst()
                          .ifPresent(albumInfo -> {
                              albumInfo.requestFocusInWindow();
                              scrollRectToVisible(albumInfo.getBounds());
                          });
                }
            }
        });
    }

    /**
     * Updates the songs currently listed, sorted by album.
     *
     * @param songs - The songs to display.
     */
    public SwingWorker listAlbums(Collection<? extends Song> songs)
    {
        SwingWorker worker = new SwingWorker<>()
        {
            /**
             * Computes a result, or throws an exception if unable
             * to do so.
             *
             * <p>
             * Note that this method is executed only once.
             *
             * <p>
             * Note: this method is executed in a background
             * thread.
             *
             * @return the computed result
             */
            @Override
            protected Object doInBackground()
            {
                try
                {
                    logger.debug("Sorting {} songs...", songs.size());
                    Map<Album, List<Song>> albums =
                            songs.stream().filter(s -> s.album != null).sorted()
                                                         .collect(Collectors
                                                                 .groupingBy(song -> song.album, Collectors
                                                                         .mapping(song -> (Song) song, Collectors
                                                                                 .toList())));
                    logger.debug("Listing {} albums ({} songs)",
                            albums.size(), songs.size());
                    GridBagConstraints c = new GridBagConstraints();
                    c.fill = GridBagConstraints.HORIZONTAL;
                    AtomicInteger i = new AtomicInteger(0);

                    SwingUtilities.invokeLater(() -> {
                        labelMap.clear();
                        artMap.clear();
                        removeAll();
                        currentAlbums = albums;
                    });

                    LinkedHashMap<JComponent, GridBagConstraints> albumInfos =
                            new LinkedHashMap<>();
                    albums.keySet().stream().sorted().forEach((album) -> {
                        List<Song> songCollection = albums.get(album);

                        AlbumInfo albumInfo = new AlbumInfo(album);
                        c.gridx = 0;
                        c.gridy = i.get();
                        c.gridwidth = 1;
                        c.gridheight = songCollection.size();
                        c.weightx = 0;
                        c.anchor = GridBagConstraints.NORTHWEST;
                        c.insets = new Insets(0, 0, 20, 10);
                        albumInfo.addMouseListener((ClickListener) e -> {
                            if (e.getClickCount() == 2)
                            {
                                Queue.getInstance().addAll(songCollection);
                            }
                        });
                        albumInfo.albumName
                                .addMouseListener((ClickListener) e -> {
                                    if (e.getClickCount() == 2)
                                    {
                                        Container inter = SongList.this;
                                        do
                                        {
                                            inter = inter.getParent();
                                        }
                                        while (!(inter instanceof Interface) && inter
                                                .getParent() != null);
                                        if (inter instanceof Interface)
                                        {
                                            ((Interface) inter)
                                                    .updateSongs(PlayerEnvironment.getSongs()
                                                            .getSongsFromAlbum(albumInfo.album));
                                        }
                                    }
                                });
                        albumInfo.artists
                                .addMouseListener((ClickListener) e -> {
                                    if (e.getClickCount() == 2)
                                    {
                                        Container inter = SongList.this;
                                        do
                                        {
                                            inter = inter.getParent();
                                        }
                                        while (!(inter instanceof Interface) && inter
                                                .getParent() != null);
                                        if (inter instanceof Interface)
                                        {
                                            ((Interface) inter)
                                                    .updateCollections(CollectionType.album, Arrays
                                                            .stream(albumInfo.album.artists)
                                                            .flatMap(s -> PlayerEnvironment.getAlbums()
                                                                    .getAlbumsFromArtist(s)
                                                                    .stream())
                                                            .collect(Collectors
                                                                    .toList()));
                                        }
                                    }
                                });
                        albumInfo.genres.addMouseListener((ClickListener) e -> {
                            if (e.getClickCount() == 2)
                            {
                                Container inter = SongList.this;
                                do
                                {
                                    inter = inter.getParent();
                                }
                                while (!(inter instanceof Interface) && inter
                                        .getParent() != null);
                                if (inter instanceof Interface)
                                {
                                    ((Interface) inter)
                                            .updateCollections(CollectionType.album, Arrays
                                                    .stream(albumInfo.album.genres)
                                                    .flatMap(s -> PlayerEnvironment.getAlbums()
                                                            .getAlbumsFromGenre(s)
                                                            .stream())
                                                    .collect(Collectors
                                                            .toList()));
                                }
                            }
                        });
                        albumInfo.year.addMouseListener((ClickListener) e -> {
                            if (e.getClickCount() == 2)
                            {
                                Container inter = SongList.this;
                                do
                                {
                                    inter = inter.getParent();
                                }
                                while (!(inter instanceof Interface) && inter
                                        .getParent() != null);
                                if (inter instanceof Interface)
                                {
                                    ((Interface) inter)
                                            .updateCollections(CollectionType.album, PlayerEnvironment.getAlbums()
                                                    .getAlbumsFromYear(albumInfo.album.year));
                                }
                            }
                        });
                        albumInfos
                                .put(albumInfo, (GridBagConstraints) c.clone());
                        artMap.put(albumInfo, album);

                        JButton firstSong = null;

                        JLabel songNum;
                        JButton songTitle;
                        AtomicInteger numSongs = new AtomicInteger();
                        for (Song song : songCollection)
                        {
                            songNum = new JLabel(String.valueOf(song.trackNum));
                            songNum.setFocusable(false);
                            c.gridx = 1;
                            c.gridy = i.get();
                            c.gridheight = 1;
                            c.weightx = 0;
                            c.anchor = GridBagConstraints.NORTHEAST;
                            c.insets = new Insets(0, 0, 0, 0);
                            albumInfos.put(songNum, (GridBagConstraints) c
                                    .clone());
                            labelMap.put(songNum, song);

                            songTitle = new JButton(song.title);
                            if (song.title == null || song.title.isEmpty())
                            {
                                if (song instanceof LocalSong)
                                {
                                    songTitle.setText(((LocalSong) song).file
                                            .getName());
                                }
                            }
                            songTitle.setHorizontalAlignment(JButton.LEFT);
                            songTitle.setFocusPainted(true);
                            songTitle.setMargin(new Insets(0, 0, 0, 0));
                            songTitle.setContentAreaFilled(false);
                            songTitle.setBorderPainted(false);
                            songTitle.setOpaque(false);
                            songTitle.addActionListener(new AbstractAction()
                            {
                                @Override
                                public void actionPerformed(ActionEvent e)
                                {
                                    Queue.getInstance().add(song);
                                    Queue.getInstance()
                                         .skipToSong(Queue.getInstance()
                                                          .size() - 1);
                                }
                            });
                            c.gridx = 2;
                            c.gridy = i.get();
                            c.weightx = 1.0;
                            c.anchor = GridBagConstraints.NORTHWEST;
                            c.insets = new Insets(0, 10, 0, 0);
                            albumInfos.put(songTitle, (GridBagConstraints) c
                                    .clone());
                            labelMap.put(songTitle, song);
                            // TODO - Add song length or something

                            if (firstSong == null)
                            {
                                firstSong = songTitle;
                                JButton finalFirstSong = firstSong;
                                albumInfo.setAction(new AbstractAction()
                                {
                                    @Override
                                    public void actionPerformed(ActionEvent e)
                                    {
                                        finalFirstSong.requestFocusInWindow();
                                    }
                                });
                                albumInfo.addKeyListener(new KeyAdapter()
                                {
                                    @Override
                                    public void keyTyped(KeyEvent e)
                                    {
                                        if (e.getKeyCode() == KeyEvent.VK_ENTER)
                                        {
                                            Queue.getInstance()
                                                 .addAll(songCollection);
                                        }
                                    }
                                });
                            }
                            i.getAndIncrement();
                            this.setProgress((int) (numSongs
                                    .incrementAndGet() / (float) songs
                                    .size() * 100F));
                        }
//            this.add(new JLabel(new ImageIcon(this.getClass().getResource("/gui/icons/defaultart.png"), "Default")), c);

                        c.gridx = 0;
                        c.gridy = i.getAndIncrement();
                        c.gridwidth = 3;
                        c.anchor = GridBagConstraints.NORTH;
                        albumInfos
                                .put(new JSeparator(SwingConstants.HORIZONTAL),
                                        (GridBagConstraints) c.clone());

                        i.getAndIncrement();
                    });
                    logger.debug("Song list built {} components",
                            albumInfos.size());
                    SwingUtilities.invokeLater(() -> {
                        albumInfos.forEach((component, c1) -> add(component, c1));
                        revalidate();
                        logger.debug("Components added");
                    });
                    return null;
                }
                catch (Exception e)
                {
                    logger.error("Could not list songs", e);
                    return null;
                }
            }
        };
        worker.execute();
        return worker;
    }

    private class SongListPolicy extends FocusTraversalPolicy
    {
        @Override
        public Component getComponentAfter(Container aContainer, Component aComponent)
        {
            Component[] children = aContainer.getComponents();
            int index = -1;
            for (int i = 0, l = aContainer
                    .getComponentCount(); index == -1 && i < l; i++)
            {
                if (children[i] == aComponent)
                {
                    index = i;
                }
            }
            if (index != -1)
            {
                if (aComponent instanceof AlbumInfo)
                {
                    for (int i = index + 1, l = aContainer
                            .getComponentCount(); i < l; i++)
                    {
                        if (children[i] instanceof AlbumInfo)
                        {
                            scrollRectToVisible(children[i].getBounds());
                            return children[i];
                        }
                    }
                }
                else if (aComponent instanceof JButton)
                {
                    for (int i = index + 1, l = aContainer
                            .getComponentCount(); i < l; i++)
                    {
                        if (children[i].isFocusable())
                        {
                            scrollRectToVisible(children[i].getBounds());
                            return children[i];
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public Component getComponentBefore(Container aContainer, Component aComponent)
        {
            Component[] children = aContainer.getComponents();
            int index = -1;
            for (int i = 0, l = aContainer
                    .getComponentCount(); index == -1 && i < l; i++)
            {
                if (children[i] == aComponent)
                {
                    index = i;
                }
            }
            if (index != -1)
            {
                if (aComponent instanceof AlbumInfo)
                {
                    for (int i = index - 1; i >= 0; i--)
                    {
                        if (children[i] instanceof AlbumInfo)
                        {
                            scrollRectToVisible(children[i].getBounds());
                            return children[i];
                        }
                    }
                }
                else if (aComponent instanceof JButton)
                {
                    for (int i = index - 1; i >= 0; i--)
                    {
                        if (children[i].isFocusable())
                        {
                            scrollRectToVisible(children[i].getBounds());
                            return children[i];
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public Component getFirstComponent(Container aContainer)
        {
            Component comp = Arrays.stream(aContainer.getComponents())
                                   .filter(a -> a instanceof AlbumInfo)
                                   .findFirst().orElse(null);
            if (comp != null)
            {
                scrollRectToVisible(comp.getBounds());
            }
            return comp;
        }

        @Override
        public Component getLastComponent(Container aContainer)
        {
            Component[] matching = Arrays.stream(aContainer.getComponents())
                                         .filter(a -> a instanceof JButton)
                                         .toArray(Component[]::new);
            if (matching.length > 0)
            {
                scrollRectToVisible(matching[matching.length - 1].getBounds());
                return matching[matching.length - 1];
            }
            else
            {
                return null;
            }
        }

        @Override
        public Component getDefaultComponent(Container aContainer)
        {
            return this.getFirstComponent(aContainer);
        }
    }
}
