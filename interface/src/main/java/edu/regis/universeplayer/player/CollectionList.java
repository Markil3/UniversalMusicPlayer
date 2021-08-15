/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;

import com.wordpress.tips4java.ScrollablePanel;
import edu.regis.universeplayer.ClickListener;
import edu.regis.universeplayer.data.Album;
import edu.regis.universeplayer.data.CollectionType;
import edu.regis.universeplayer.data.Song;
import edu.regis.universeplayer.data.SongProvider;

/**
 * This panel will list all the song collections on display (albums, artists, genres, etc.)
 *
 * @author William Hubbard
 * @version 0.1
 */
public class CollectionList extends ScrollablePanel
{
    /**
     * The type of collections being displayed.
     */
    private CollectionType type;
    /**
     * A link between the JLabel and the object they point towards.
     */
    private Map<JButton, Object> labelMap = new HashMap<>();
    
    /**
     * A list of all things interested in knowing when we click a collection.
     */
    private LinkedList<SongDisplayListener> listeners = new LinkedList<>();
    
    /**
     * Creates a collections list view.
     */
    public CollectionList()
    {
        super();
    
        FlowLayout layout = new FlowLayout();
        this.setLayout(layout);
        this.setFocusCycleRoot(true);
//        this.setFocusable(true);
        this.setScrollableWidth(ScrollableSizeHint.FIT);
        this.setScrollableHeight(ScrollableSizeHint.STRETCH);
        this.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                labelMap.keySet().stream().findFirst().ifPresent(JButton::requestFocusInWindow);
            }
        });
    }
    
    /**
     * Updates the collections currently listed, sorted by album.
     *
     * @param type    - The type of collection we are displaying.
     * @param objects - The collection to display.
     */
    public void listCollection(CollectionType type, Collection<?> objects)
    {
        Class<?> fType = objects.stream().filter(Objects::nonNull).map(Object::getClass).findFirst()
                .orElse(null);
        if (objects.isEmpty() || fType == null)
        {
            /*
             * We really don't need error checking here, and we couldn't get it working anyway.
             */
            this.labelMap.clear();
            this.removeAll();
            return;
        }
        if (!type.objectType.isAssignableFrom(fType))
        {
            throw new ClassCastException("Can't assign " + type.objectType
                    .getName() + " from " + fType
                    .getName());
        }
        
        this.labelMap.clear();
        this.removeAll();
        this.type = type;
        switch (type)
        {
        case album -> this.addAlbums(objects.stream().sorted().map(ob -> (Album) ob)
                .collect(Collectors.toList()));
        case artist -> this.addArtists(objects.stream().sorted().map(ob -> (String) ob)
                .collect(Collectors.toList()), false);
        case albumArtist -> this.addArtists(objects.stream().sorted().map(ob -> (String) ob)
                .collect(Collectors.toList()), true);
        case genre -> this.addGenres(objects.stream().sorted().map(ob -> (String) ob)
                .collect(Collectors.toList()));
        case year -> this.addYears(objects.stream().sorted().map(ob -> (Integer) ob)
                .collect(Collectors.toList()));
        }
    }
    
    /**
     * Updates the display to show a list of artists
     *
     * @param artists - The list of artists to display.
     * @param album   - Whether the list should be treated as song artists or album artists.
     */
    private void addArtists(List<String> artists, boolean album)
    {
        final int ART_SIZE = 128;
        
        JButton artistLabel;
        ImageIcon icon;
        
        for (String artist : artists)
        {
            artistLabel = new JButton();
            setButtonLook(artistLabel);
            // TODO - Maybe add some sort of artist image lookup?
//            if (album.art != null)
//            {
//                icon = album.art;
//            }
//            else
//            {
            icon = new ImageIcon(this.getClass()
                    .getResource("/gui/icons/artist.png"), "Default");
//            }
            icon.setImage(icon.getImage().getScaledInstance(ART_SIZE, ART_SIZE, 0));
            artistLabel.setIcon(icon);
            artistLabel.setText(artist);
            artistLabel.setHorizontalTextPosition(JLabel.CENTER);
            artistLabel.setVerticalTextPosition(JLabel.BOTTOM);
            artistLabel.addActionListener(mouseEvent -> {
                if (album)
                {
                    this.triggerSongDisplayListeners(SongProvider.INSTANCE
                            .getAlbumsFromArtist(artist).stream()
                            .flatMap(album2 -> SongProvider.INSTANCE.getSongsFromAlbum(album2)
                                    .stream())
                            .collect(Collectors.toList()));
                }
                else
                {
                    this.triggerSongDisplayListeners(new ArrayList<>(SongProvider.INSTANCE
                            .getSongsFromArtist(artist)));
                }
            });
            this.add(artistLabel);
            this.labelMap.put(artistLabel, artist);
        }
    }
    
    /**
     * Updates the display to show a list of albums.
     *
     * @param albums - The list of albums to display.
     */
    private void addAlbums(List<Album> albums)
    {
        final int ART_SIZE = 128;
        
        JButton albumLabel;
        ImageIcon icon;
        
        for (Album album : albums)
        {
            albumLabel = new JButton();
            setButtonLook(albumLabel);
            icon = Objects.requireNonNullElseGet(album.art, () -> new ImageIcon(this.getClass()
                    .getResource("/gui/icons/defaultart.png"), "Default"));
            icon.setImage(icon.getImage().getScaledInstance(ART_SIZE, ART_SIZE, 0));
            albumLabel.setIcon(icon);
            albumLabel.setText(album.name);
            albumLabel.setHorizontalTextPosition(JLabel.CENTER);
            albumLabel.setVerticalTextPosition(JLabel.BOTTOM);
            albumLabel.addActionListener(mouseEvent -> this.triggerSongDisplayListeners(new ArrayList<>(SongProvider.INSTANCE
                    .getSongsFromAlbum(album))));
            this.add(albumLabel);
            this.labelMap.put(albumLabel, album);
        }
    }
    
    /**
     * Updates the display to show a list of genres.
     *
     * @param genres - The list of genres to display.
     */
    private void addGenres(List<String> genres)
    {
//        final int ART_SIZE = 128;
        
        JButton genreLabel;
//        ImageIcon icon;
        
        for (String genre : genres)
        {
            genreLabel = new JButton();
            setButtonLook(genreLabel);
            // TODO - Maybe add some sort of artist image lookup?
//            if (album.art != null)
//            {
//                icon = album.art;
//            }
//            else
//            {
//                icon = new ImageIcon(this.getClass()
//                                         .getResource("/gui/icons/defaultart.png"), "Default");
//            }
//            icon.setImage(icon.getImage().getScaledInstance(ART_SIZE, ART_SIZE, 0));
//            albumLabel.setIcon(icon);
            genreLabel.setText(genre);
            genreLabel.setHorizontalTextPosition(JLabel.CENTER);
            genreLabel.setVerticalTextPosition(JLabel.BOTTOM);
            genreLabel.addActionListener(mouseEvent -> this.triggerSongDisplayListeners(SongProvider.INSTANCE
                    .getAlbumsFromGenre(genre).stream()
                    .flatMap(album2 -> SongProvider.INSTANCE.getSongsFromAlbum(album2)
                            .stream())
                    .collect(Collectors.toList())));
            this.add(genreLabel);
            this.labelMap.put(genreLabel, genre);
        }
    }
    
    /**
     * Updates the display to show a list of release years.
     *
     * @param years - The list of genres to display.
     */
    private void addYears(List<Integer> years)
    {
//        final int ART_SIZE = 128;
    
        JButton yearLabel;
//        ImageIcon icon;
        
        for (Integer year : years)
        {
            yearLabel = new JButton();
            setButtonLook(yearLabel);
            // TODO - Maybe add some sort of artist image lookup?
//            if (album.art != null)
//            {
//                icon = album.art;
//            }
//            else
//            {
//                icon = new ImageIcon(this.getClass()
//                                         .getResource("/gui/icons/defaultart.png"), "Default");
//            }
//            icon.setImage(icon.getImage().getScaledInstance(ART_SIZE, ART_SIZE, 0));
//            albumLabel.setIcon(icon);
            yearLabel.setText(year.toString());
            yearLabel.setHorizontalTextPosition(JLabel.CENTER);
            yearLabel.setVerticalTextPosition(JLabel.BOTTOM);
            yearLabel.addActionListener(mouseEvent -> this.triggerSongDisplayListeners(SongProvider.INSTANCE
                    .getAlbumsFromYear(year).stream()
                    .flatMap(album2 -> SongProvider.INSTANCE.getSongsFromAlbum(album2)
                            .stream())
                    .collect(Collectors.toList())));
            this.add(yearLabel);
            this.labelMap.put(yearLabel, year);
        }
    }
    
    private void setButtonLook(JButton button)
    {
        button.setFocusPainted(true);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
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
}
