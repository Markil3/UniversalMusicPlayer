/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.LinkedList;

import javax.swing.*;

/**
 * This panel contains the buttons necessary for controlling the playback of audio.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class PlayerControls extends JPanel
{
    private final JButton playButton;
    private final JButton nextButton;
    private final JButton prevButton;
    private final JSlider progress;
    private final JProgressBar updateProgress;

    /**
     * A list of all things interested in knowing when we trigger a command.
     */
    private final LinkedList<PlaybackCommandListener> listeners = new LinkedList<>();

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
        this.setFocusable(true);
        this.setFocusCycleRoot(false);

        buttonLayout = new FlowLayout();
        buttonCont = new JPanel(buttonLayout);
        this.add(buttonCont);

        this.prevButton = new JButton();
        icon = new ImageIcon(this.getClass()
                                 .getResource("/gui/icons/skipPrev.png"), "Previous Button");
        icon.setImage(icon.getImage().getScaledInstance(ICON_SIZE.width, ICON_SIZE.height, 0));
        this.prevButton.setIcon(icon);
        this.prevButton.setPreferredSize(BUTTON_SIZE);
        this.prevButton.addActionListener(actionEvent -> this.triggerCommandListeners(PlaybackCommand.PREVIOUS, null));
        buttonCont.add(this.prevButton);

        this.playButton = new JButton();
        icon = new ImageIcon(this.getClass().getResource("/gui/icons/play.png"), "Play Button");
        icon.setImage(icon.getImage().getScaledInstance(ICON_SIZE.width, ICON_SIZE.height, 0));
        this.playButton.setIcon(icon);
        this.playButton.setPreferredSize(BUTTON_SIZE);
        this.playButton.addActionListener(actionEvent -> this.triggerCommandListeners(PlaybackCommand.PLAY, null));
        buttonCont.add(this.playButton);

        this.nextButton = new JButton();
        icon = new ImageIcon(this.getClass().getResource("/gui/icons/skipNext.png"), "Next Button");
        icon.setImage(icon.getImage().getScaledInstance(ICON_SIZE.width, ICON_SIZE.height, 0));
        this.nextButton.setIcon(icon);
        this.nextButton.setPreferredSize(BUTTON_SIZE);
        this.nextButton.addActionListener(actionEvent -> this.triggerCommandListeners(PlaybackCommand.NEXT, null));
        buttonCont.add(this.nextButton);

        progressLayout = new SpringLayout();
        progressCont = new JPanel(progressLayout);
        this.add(progressCont);

        this.progress = new JSlider();
        this.progress.addChangeListener(changeEvent -> this.triggerCommandListeners(PlaybackCommand.SEEK, this.progress.getValue()));
        this.add(this.progress);
    
        this.updateProgress = new JProgressBar();
        this.updateProgress.setStringPainted(true);
        this.setUpdateProgress(0, 0, null);
        this.add(this.updateProgress);
    
        this.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
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
                /*
                 * Only auto-switch focus if the previous component was not from
                 * within.
                 */
                if (index == -1)
                {
                    prevButton.requestFocusInWindow();
                }
            }
        });

        layout.putConstraint(SpringLayout.NORTH, buttonCont, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.WEST, buttonCont, 0, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.EAST, buttonCont, 0, SpringLayout.EAST, this);
        layout.putConstraint(SpringLayout.NORTH, this.progress, 5, SpringLayout.SOUTH, buttonCont);
        layout.putConstraint(SpringLayout.WEST, this.progress, 5, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.EAST, this.progress, 5, SpringLayout.EAST, this);
        layout.putConstraint(SpringLayout.NORTH, this.updateProgress, 5, SpringLayout.SOUTH, this.progress);
        layout.putConstraint(SpringLayout.WEST, this.updateProgress, 5, SpringLayout.WEST, this);
    
        layout.putConstraint(SpringLayout.EAST, this, 5, SpringLayout.EAST, this.updateProgress);
        layout.putConstraint(SpringLayout.SOUTH, this, 5, SpringLayout.SOUTH, this.updateProgress);
    }
    
    void setUpdateProgress(int updated, int toUpdate, String updating)
    {
        this.updateProgress.setString(updating);
        if (toUpdate == 0)
        {
            this.updateProgress.setVisible(false);
//            this.updateProgress.setPreferredSize(new Dimension(this.updateProgress.getPreferredSize().width, 0));
        }
        else
        {
            this.updateProgress.setVisible(true);
//            this.updateProgress.setSize(new Dimension(this.updateProgress.getPreferredSize().width, this.updateSize));
            if (toUpdate < 0)
            {
                this.updateProgress.setIndeterminate(true);
            }
            else
            {
                this.updateProgress.setIndeterminate(false);
                this.updateProgress.setMaximum(toUpdate);
                this.updateProgress.setValue(updated);
            }
        }
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
