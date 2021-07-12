/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universe_player.player;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.LinkedList;

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

    /**
     * A list of all things interested in knowing when we trigger a command.
     */
    private LinkedList<PlaybackCommandListener> listeners = new LinkedList<>();

    public PlayerControls()
    {
        final Dimension BUTTON_SIZE = new Dimension(32, 32);
        final Dimension ICON_SIZE = new Dimension(16, 16);
        ImageIcon icon;
        JPanel buttonCont, progressCont;
        FlowLayout buttonLayout;
        SpringLayout progressLayout;

        SpringLayout layout = new SpringLayout();
        this.setLayout(layout);

        buttonLayout = new FlowLayout();
        buttonCont = new JPanel(buttonLayout);
        this.add(buttonCont);

        this.prevButton = new JButton();
        icon = new ImageIcon(this.getClass()
                                 .getResource("/gui/icons/skipPrev.png"), "Previous Button");
        icon.setImage(icon.getImage().getScaledInstance(ICON_SIZE.width, ICON_SIZE.height, 0));
        this.prevButton.setIcon(icon);
        this.prevButton.setPreferredSize(BUTTON_SIZE);
        this.prevButton.addActionListener(actionEvent -> {
            this.triggerCommandListeners(PlaybackCommand.PREVIOUS, null);
        });
        buttonCont.add(this.prevButton);

        this.playButton = new JButton();
        icon = new ImageIcon(this.getClass().getResource("/gui/icons/play.png"), "Play Button");
        icon.setImage(icon.getImage().getScaledInstance(ICON_SIZE.width, ICON_SIZE.height, 0));
        this.playButton.setIcon(icon);
        this.playButton.setPreferredSize(BUTTON_SIZE);
        this.playButton.addActionListener(actionEvent -> {
            this.triggerCommandListeners(PlaybackCommand.PLAY, null);
        });
        buttonCont.add(this.playButton);

        this.nextButton = new JButton();
        icon = new ImageIcon(this.getClass().getResource("/gui/icons/skipNext.png"), "Next Button");
        icon.setImage(icon.getImage().getScaledInstance(ICON_SIZE.width, ICON_SIZE.height, 0));
        this.nextButton.setIcon(icon);
        this.nextButton.setPreferredSize(BUTTON_SIZE);
        this.nextButton.addActionListener(actionEvent -> {
            this.triggerCommandListeners(PlaybackCommand.NEXT, null);
        });
        buttonCont.add(this.nextButton);

        progressLayout = new SpringLayout();
        progressCont = new JPanel(progressLayout);
        this.add(progressCont);

        this.progress = new JSlider();
        this.progress.addChangeListener(changeEvent -> {
            this.triggerCommandListeners(PlaybackCommand.SEEK, this.progress.getValue());
        });
        this.add(this.progress);

        layout.putConstraint(SpringLayout.NORTH, buttonCont, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.WEST, buttonCont, 0, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, this.progress, 5, SpringLayout.SOUTH, buttonCont);
        layout.putConstraint(SpringLayout.WEST, this.progress, 5, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.EAST, this, 5, SpringLayout.EAST, this.progress);
        layout.putConstraint(SpringLayout.SOUTH, this, 5, SpringLayout.SOUTH, this.progress);
        layout.putConstraint(SpringLayout.EAST, buttonCont, 0, SpringLayout.EAST, this);
    }

    /**
     * Adds a listener for playback commands.
     *
     * @param listener - The listener to add.
     */
    public void addCommandListener(PlaybackCommandListener listener)
    {
        this.listeners.add(listener);
    }

    /**
     * Removes a playback listener.
     *
     * @param listener - The listener to remove.
     */
    public void removeCommandListener(PlaybackCommandListener listener)
    {
        this.listeners.remove(listener);
    }

    /**
     * Triggers all the command listeners.
     *
     * @param command - The command to trigger.
     * @param data    - Extra command data.
     */
    protected void triggerCommandListeners(PlaybackCommand command, Object data)
    {
        for (PlaybackCommandListener listener : this.listeners)
        {
            listener.onCommand(command, data);
        }
    }
}
