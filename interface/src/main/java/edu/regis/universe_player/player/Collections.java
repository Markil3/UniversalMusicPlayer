/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universe_player.player;

import java.awt.Color;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This panel links to various song collections the player has set up.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class Collections extends JPanel
{
    JLabel allSongs;
    JLabel artists;
    JLabel albums;
    JLabel genres;
    JLabel years;
    JLabel playlists;

    public Collections()
    {
        JLabel label;
        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        this.setLayout(layout);

        this.add(this.allSongs = label = new JLabel("All Songs"));
        label.setForeground(Color.BLUE);
        this.add(this.artists = label = new JLabel("Artists"));
        label.setForeground(Color.BLUE);
        this.add(this.albums = label = new JLabel("Albums"));
        label.setForeground(Color.BLUE);
        this.add(this.genres = label = new JLabel("Genres"));
        label.setForeground(Color.BLUE);
        this.add(this.years = label = new JLabel("Years"));
        label.setForeground(Color.BLUE);
        this.add(new JLabel("\u23AF\u23AF\u23AF\u23AF\u23AF\u23AF"));
        this.add(this.playlists = label = new JLabel("Playlists"));
        label.setForeground(Color.BLUE);
    }
}
