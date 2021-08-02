/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.regis.universeplayer.data.Album;
import edu.regis.universeplayer.data.Song;
import edu.regis.universeplayer.data.SongProvider;

/**
 * This panel will list all the songs that are to be currently displayed.
 */
public class SongList extends JPanel
{
    private Map<Album, List<Song>> currentAlbums;
    private Map<JLabel, Song> labelMap = new HashMap<>();
    private Map<AlbumInfo, Album> artMap = new HashMap<>();

    public SongList()
    {
        super();

        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);

        SongProvider<?> provider = SongProvider.INSTANCE;
        this.listAlbums(provider.getSongs());
    }

    /**
     * Updates the songs currently listed, sorted by album.
     *
     * @param songs - The songs to display.
     */
    public void listAlbums(Collection<? extends Song> songs)
    {
        Map<Album, List<Song>> albums = songs.stream().sorted().collect(Collectors
                .groupingBy(song -> song.album, Collectors
                        .mapping(song -> (Song) song, Collectors.toList())));
        GridBagConstraints c = new GridBagConstraints(), c2 = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 20, 0);
        int i = 0, j;
        AlbumInfo albumInfo;
        List<Song> songCollection;
        JPanel songList;
        JLabel songNum, songTitle;

        this.labelMap.clear();
        this.artMap.clear();
        this.removeAll();
        this.currentAlbums = albums;

        for (Album album : albums.keySet())
        {
            songCollection = albums.get(album);

            albumInfo = new AlbumInfo(album);
            c.gridx = 0;
            c.gridy = i;
            c.anchor = GridBagConstraints.WEST;
            this.add(albumInfo, c);
            this.artMap.put(albumInfo, album);

            songList = new JPanel(new GridBagLayout());
            j = 0;
            for (Song song : songCollection)
            {
                songNum = new JLabel(String.valueOf(song.trackNum));
                c2.gridx = 0;
                c2.gridy = j;
                c2.anchor = GridBagConstraints.EAST;
                c2.insets = new Insets(0, 0, 0, 0);
                songList.add(songNum, c2);
                this.labelMap.put(songNum, song);

                songTitle = new JLabel(song.title);
                c2.gridx = 1;
                c2.gridy = j;
                c2.anchor = GridBagConstraints.WEST;
                c2.insets = new Insets(0, 10, 0, 0);
                songList.add(songTitle, c2);
                this.labelMap.put(songTitle, song);
                // TODO - Add song length or something

                j++;
            }
            c.gridx = 1;
            c.anchor = GridBagConstraints.WEST;
            this.add(songList, c);
//            this.add(new JLabel(new ImageIcon(this.getClass().getResource("/gui/icons/defaultart.png"), "Default")), c);

            i++;
        }
    }
}
