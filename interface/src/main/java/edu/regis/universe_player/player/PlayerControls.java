/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universe_player.player;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SpringLayout;

/**
 * This panel contains the buttons necessary for controlling the playback of audio.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class PlayerControls extends JPanel
{
    private JButton playButton;
    private JButton nextButton;
    private JButton prevButton;
    private JSlider progress;

    public PlayerControls()
    {
        final Dimension BUTTON_SIZE = new Dimension(32, 32);
        final Dimension ICON_SIZE = new Dimension(16, 16);
        ImageIcon icon;
        JButton button;
        JPanel buttonCont, progressCont;
        FlowLayout buttonLayout;
        SpringLayout progressLayout;

        SpringLayout layout = new SpringLayout();
        this.setLayout(layout);

        buttonLayout = new FlowLayout();
        buttonCont = new JPanel(buttonLayout);
        this.add(buttonCont);

        button = new JButton();
        icon = new ImageIcon(this.getClass()
                                 .getResource("/gui/icons/skipPrev.png"), "Previous Button");
        icon.setImage(icon.getImage().getScaledInstance(ICON_SIZE.width, ICON_SIZE.height, 0));
        button.setIcon(icon);
        button.setPreferredSize(BUTTON_SIZE);
        buttonCont.add(button);
        this.prevButton = button;

        button = new JButton();
        icon = new ImageIcon(this.getClass().getResource("/gui/icons/play.png"), "Play Button");
        icon.setImage(icon.getImage().getScaledInstance(ICON_SIZE.width, ICON_SIZE.height, 0));
        button.setIcon(icon);
        button.setPreferredSize(BUTTON_SIZE);
        buttonCont.add(button);
        this.playButton = button;

        button = new JButton();
        icon = new ImageIcon(this.getClass().getResource("/gui/icons/skipNext.png"), "Next Button");
        icon.setImage(icon.getImage().getScaledInstance(ICON_SIZE.width, ICON_SIZE.height, 0));
        button.setIcon(icon);
        button.setPreferredSize(BUTTON_SIZE);
        buttonCont.add(button);
        this.nextButton = button;

        progressLayout = new SpringLayout();
        progressCont = new JPanel(progressLayout);
        this.add(progressCont);

        this.progress = new JSlider();
        this.add(this.progress);

        layout.putConstraint(SpringLayout.NORTH, buttonCont, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.WEST, buttonCont, 0, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, this.progress, 5, SpringLayout.SOUTH, buttonCont);
        layout.putConstraint(SpringLayout.WEST, this.progress, 5, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.EAST, this, 5, SpringLayout.EAST, this.progress);
        layout.putConstraint(SpringLayout.SOUTH, this, 5, SpringLayout.SOUTH, this.progress);
        layout.putConstraint(SpringLayout.EAST, buttonCont, 0, SpringLayout.EAST, this);
    }
}
