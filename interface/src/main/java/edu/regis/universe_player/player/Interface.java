/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universe_player.player;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

/**
 * The Interface class serves as the primary GUI that the player interacts with.
 * @author William Hubbard
 * @version 0.1
 */
public class Interface extends JFrame
{
    public static void main(String[] args)
    {
        Interface inter = new Interface();
        inter.pack();
        inter.setVisible(true);
    }

    public Interface()
    {
        super();
        this.setTitle("Universal Music Player");
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(new Collections(), BorderLayout.LINE_START);
        this.getContentPane().add(new PlayerControls(), BorderLayout.PAGE_END);

        JScrollPane songList = new JScrollPane(new SongList());
        this.getContentPane().add(songList, BorderLayout.CENTER);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}