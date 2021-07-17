/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import edu.regis.universeplayer.data.Album;

/**
 * This panel will display information on an album.
 */
public class AlbumInfo extends JPanel
{
    private Album album;
    private JLabel artLabel;
    private JLabel albumName;
    private JLabel artists;
    private JLabel genres;
    private JLabel year;

    public AlbumInfo()
    {
        SpringLayout infoLayout = new SpringLayout();
        this.setLayout(infoLayout);

        this.artLabel = new JLabel();
        this.add(this.artLabel);
        this.albumName = new JLabel("Album");
        this.add(this.albumName);
        this.artists = new JLabel("Artists");
        this.add(this.artists);
        this.genres = new JLabel("Genres");
        this.add(this.genres);
        this.year = new JLabel("20XX");
        this.add(this.year);

        /*
         * Set the layout information
         */
        infoLayout.putConstraint(SpringLayout.NORTH, artLabel, 5, SpringLayout.NORTH, this);
        infoLayout.putConstraint(SpringLayout.WEST, artLabel, 5, SpringLayout.WEST, this);
        infoLayout.putConstraint(SpringLayout.NORTH, albumName, 5, SpringLayout.NORTH, this);
        infoLayout.putConstraint(SpringLayout.WEST, albumName, 5, SpringLayout.EAST, artLabel);
        infoLayout.putConstraint(SpringLayout.NORTH, artists, 5, SpringLayout.SOUTH, albumName);
        infoLayout.putConstraint(SpringLayout.WEST, artists, 5, SpringLayout.EAST, artLabel);
        infoLayout.putConstraint(SpringLayout.NORTH, genres, 5, SpringLayout.SOUTH, artists);
        infoLayout.putConstraint(SpringLayout.WEST, genres, 5, SpringLayout.EAST, artLabel);
        infoLayout.putConstraint(SpringLayout.NORTH, year, 5, SpringLayout.SOUTH, genres);
        infoLayout.putConstraint(SpringLayout.WEST, year, 5, SpringLayout.EAST, artLabel);
//        infoLayout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, albumName);
        infoLayout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, artists);
//        infoLayout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, genres);
//        infoLayout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, year);
        infoLayout.putConstraint(SpringLayout.SOUTH, this, 5, SpringLayout.EAST, artLabel);
    }

    public AlbumInfo(Album album)
    {
        this();
        this.updateInfo(album);
    }

    public void updateInfo(Album album)
    {
        final int ART_SIZE = 128;
        ImageIcon icon;
        StringBuilder builder;

        this.album = album;

        if (album.art != null)
        {
            icon = album.art;
        }
        else
        {
            icon = new ImageIcon(this.getClass()
                                     .getResource("/gui/icons/defaultart.png"), "Default");
        }
        icon.setImage(icon.getImage().getScaledInstance(ART_SIZE, ART_SIZE, 0));
        this.artLabel.setIcon(icon);

        this.albumName.setText(album.name);

        builder = new StringBuilder();
        if (album.artists != null && album.artists.length >= 1)
        {
            builder.append(album.artists[0]);
            for (int i = 1, l = album.artists.length - 1; i < l; i++)
            {
                builder.append(", ");
                builder.append(album.artists[i]);
            }
            if (album.artists.length > 1)
            {
                builder.append(" & ");
                builder.append(album.artists[album.artists.length - 1]);
            }
        }
        this.artists.setText(builder.toString());

        builder = new StringBuilder();
        if (album.genres != null && album.genres.length >= 1)
        {
            builder.append(album.genres[0]);
            for (int i = 1, l = album.genres.length - 1; i < l; i++)
            {
                builder.append(", ");
                builder.append(album.genres[i]);
            }
            if (album.genres.length > 1)
            {
                builder.append(" & ");
                builder.append(album.genres[album.genres.length - 1]);
            }
        }
        this.genres.setText(builder.toString());

        this.year.setText(String.valueOf(album.year));
    }
}
