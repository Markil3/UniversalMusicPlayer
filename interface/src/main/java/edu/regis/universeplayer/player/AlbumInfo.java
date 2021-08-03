/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import javax.swing.*;
import javax.swing.border.LineBorder;

import edu.regis.universeplayer.data.Album;

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

/**
 * This panel will display information on an album.
 */
public class AlbumInfo extends JButton
{
    public Album album;
    public final JLabel artLabel;
    public final JLabel albumName;
    public final JLabel artists;
    public final JLabel genres;
    public final JLabel year;
    
    public AlbumInfo()
    {
        this.removeAll();
        this.setContentAreaFilled(false);
        this.setBorder(null);
        SpringLayout infoLayout = new SpringLayout();
        this.setLayout(infoLayout);
        this.setFocusable(true);
        
        this.setModel(new DefaultButtonModel());
        
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
        
        int maxLength = -1;
        JLabel longest = null;
        for (JLabel label : Arrays.asList(albumName, artists, genres, year))
        {
            if (label.getWidth() > maxLength)
            {
                longest = label;
                maxLength = longest.getWidth();
            }
        }
        infoLayout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, longest);
        infoLayout.putConstraint(SpringLayout.SOUTH, this, 5, SpringLayout.EAST, artLabel);
        
        while (this.getMouseListeners().length > 0)
        {
            this.removeMouseListener(this.getMouseListeners()[0]);
        }
        this.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                ((AlbumInfo) e.getComponent()).setBorder(new LineBorder(Color.GRAY, 1));
            }
            
            @Override
            public void focusLost(FocusEvent e)
            {
                ((AlbumInfo) e.getComponent()).setBorder(null);
            }
        });
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
