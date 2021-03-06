/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.gui;

import edu.regis.universeplayer.PlaybackListener;
import edu.regis.universeplayer.PlaybackStatus;
import edu.regis.universeplayer.player.Player;
import edu.regis.universeplayer.browserCommands.CommandConfirmation;
import edu.regis.universeplayer.browserCommands.QueryFuture;
import edu.regis.universeplayer.data.PlaybackEvent;
import edu.regis.universeplayer.data.Queue;
import edu.regis.universeplayer.data.Song;
import edu.regis.universeplayer.player.PlayerManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * This panel contains the buttons necessary for controlling the playback of
 * audio.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class PlayerControls extends JPanel implements PlaybackListener
{
    private static final Logger logger = LoggerFactory
            .getLogger(PlayerControls.class);
    private static final ResourceBundle langs = ResourceBundle
            .getBundle("lang.interface", Locale.getDefault());
    private final ImageIcon PLAY_ICON, PAUSE_ICON;

    private final JButton playButton;
    private final JButton nextButton;
    private final JButton prevButton;
    private final JProgressBar progress;
    private final JProgressBar updateProgress;

    private final ForkJoinPool service = new ForkJoinPool();

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

        this.prevButton = new JButton(Interface.getInstance().actions
                .get("playback.skipPrev"));
        this.prevButton.setText("");
        icon = new ImageIcon(this.getClass()
                                 .getResource("/gui/icons/skipPrev.png"), "Previous Button");
        icon.setImage(icon.getImage()
                          .getScaledInstance(ICON_SIZE.width, ICON_SIZE.height, 0));
        this.prevButton.setIcon(icon);
        this.prevButton.setPreferredSize(BUTTON_SIZE);
        buttonCont.add(this.prevButton);

        this.playButton = new JButton(Interface.getInstance().actions
                .get("playback.toggle"));
        this.playButton.setText("");
        PAUSE_ICON = icon = new ImageIcon(this.getClass()
                                              .getResource("/gui/icons/pause.png"), "Pause Button");
        icon.setImage(icon.getImage()
                          .getScaledInstance(ICON_SIZE.width, ICON_SIZE.height, 0));
        PLAY_ICON = icon = new ImageIcon(this.getClass()
                                             .getResource("/gui/icons/play.png"), "Play Button");
        icon.setImage(icon.getImage()
                          .getScaledInstance(ICON_SIZE.width, ICON_SIZE.height, 0));
        this.playButton.setIcon(icon);
        this.playButton.setPreferredSize(BUTTON_SIZE);
        buttonCont.add(this.playButton);

        this.nextButton = new JButton(Interface.getInstance().actions
                .get("playback.skipNext"));
        this.nextButton.setText("");
        icon = new ImageIcon(this.getClass()
                                 .getResource("/gui/icons/skipNext.png"), "Next Button");
        icon.setImage(icon.getImage()
                          .getScaledInstance(ICON_SIZE.width, ICON_SIZE.height, 0));
        this.nextButton.setIcon(icon);
        this.nextButton.setPreferredSize(BUTTON_SIZE);
        buttonCont.add(this.nextButton);

        progressLayout = new SpringLayout();
        progressCont = new JPanel(progressLayout);
        this.add(progressCont);

        this.progress = new JProgressBar();
        this.progress.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                seek((float) e.getX() / (float) e.getComponent()
                                                 .getWidth() * ((JProgressBar) e
                        .getComponent()).getMaximum());
                logger.debug("Changing time");
            }
        });
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
                Component[] children = ((Container) e.getComponent())
                        .getComponents();
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

        layout.putConstraint(SpringLayout.EAST, this, 5, SpringLayout.EAST, this.progress);
        layout.putConstraint(SpringLayout.EAST, this, 5, SpringLayout.EAST, this.updateProgress);
        layout.putConstraint(SpringLayout.SOUTH, this, 5, SpringLayout.SOUTH, this.updateProgress);

        PlayerManager.getPlayers().addPlaybackListener(this);
    }

    private void seek(float value)
    {
        this.service.execute(() -> {
            ForkJoinTask<Void> status = PlayerManager.getPlayers().seek(value);
            status.join();
            if (status.isCompletedAbnormally())
            {
                logger.error("Could not run command",
                        status.getException());
                JOptionPane.showMessageDialog(this,
                        status.getException(),
                        status.getException().getMessage(),
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        this.triggerCommandListeners(PlaybackCommand.SEEK, value);
    }

    protected void play()
    {
        this.service.execute(() -> {
            try
            {
                ForkJoinTask status =
                        PlayerManager.getPlayers().getStatus();
                status.join();
                if (status.isCompletedNormally())
                {
                    switch ((PlaybackStatus) status.get())
                    {
                    case PAUSED -> status = PlayerManager.getPlayers()
                                                               .play();
                    case STOPPED, EMPTY -> {
                        if (Queue.getInstance().size() > 0)
                        {
                            if (Queue.getInstance()
                                     .getCurrentSong() == null)
                            {
                                Queue.getInstance().skipToSong(0);
                                status = null;
                            }
                            else
                            {
                                status = PlayerManager.getPlayers().play();
                            }
                        }
                    }
                    default -> {
                        status = null;
                    }
                    }
                    if (status != null)
                    {
                        status.join();
                    }
                }
                if (status != null && status.isCompletedAbnormally())
                {
                    logger.error("Could not run command",
                            status.getException());
                    JOptionPane.showMessageDialog(this,
                            status.getException(),
                            status.getException().getMessage(),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
            catch (ExecutionException | InterruptedException e)
            {
                logger.error("Could not get current playback status", e);
                JOptionPane.showMessageDialog(this, e.getMessage(), langs
                        .getString(
                                "error.command"), JOptionPane.ERROR_MESSAGE);
            }
        });
        this.triggerCommandListeners(PlaybackCommand.PLAY, null);
    }

    protected void pause()
    {
        this.service.execute(() -> {
            try
            {
                ForkJoinTask status =
                        PlayerManager.getPlayers().getStatus();
                status.join();
                if (status.isCompletedNormally())
                {
                    if (status.get() == PlaybackStatus.PLAYING)
                    {
                        status =
                                PlayerManager.getPlayers().pause();
                        status.join();
                    }
                }
                if (status.isCompletedAbnormally())
                {
                    logger.error("Could not run command",
                            status.getException());
                    JOptionPane.showMessageDialog(this,
                            status.getException(),
                            status.getException().getMessage(),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
            catch (ExecutionException | InterruptedException e)
            {
                logger.error("Could not get current playback status", e);
                JOptionPane.showMessageDialog(this, e.getMessage(), langs
                        .getString(
                                "error.command"), JOptionPane.ERROR_MESSAGE);
            }
        });
        this.triggerCommandListeners(PlaybackCommand.PAUSE, null);
    }

    /**
     * Toggles the playback of the current song.
     */
    protected void togglePlayback()
    {
        this.service.execute(() -> {
            try
            {
                ForkJoinTask status =
                        PlayerManager.getPlayers().getStatus();
                status.join();
                if (status.isCompletedNormally())
                {
                    switch ((PlaybackStatus) status.get())
                    {
                    case PAUSED -> status = PlayerManager.getPlayers()
                                                               .play();
                    case PLAYING -> status = PlayerManager.getPlayers()
                                                                .pause();
                    case STOPPED, EMPTY -> {
                        if (Queue.getInstance().size() > 0)
                        {
                            if (Queue.getInstance()
                                     .getCurrentSong() == null)
                            {
                                Queue.getInstance().skipToSong(0);
                                status = null;
                            }
                            else
                            {
                                status = PlayerManager.getPlayers().play();
                            }
                        }
                    }
                    default -> status = null;
                    }
                    if (status != null)
                    {
                        status.join();
                    }
                }
                if (status != null && status.isCompletedAbnormally())
                {
                    logger.error("Could not run command",
                            status.getException());
                    JOptionPane.showMessageDialog(this,
                            status.getException(),
                            status.getException().getMessage(),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
            catch (ExecutionException | InterruptedException e)
            {
                logger.error("Could not get current playback status", e);
                JOptionPane.showMessageDialog(this, e.getMessage(), langs
                        .getString(
                                "error.command"), JOptionPane.ERROR_MESSAGE);
            }
        });
        this.triggerCommandListeners(PlaybackCommand.PLAY, null);
    }

    /**
     * Skips to the next song.
     */
    protected void previousSong()
    {
        Queue.getInstance().skipPrev();
        this.triggerCommandListeners(PlaybackCommand.PREVIOUS, null);
    }

    /**
     * Skips to the next song.
     */
    protected void nextSong()
    {
        Queue.getInstance().skipNext();
        this.triggerCommandListeners(PlaybackCommand.NEXT, null);
    }

    void setUpdateProgress(int updated, int toUpdate, String updating)
    {
        this.updateProgress.setString(updating);
        if (toUpdate == 0 || updated == toUpdate)
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
        this.repaint();
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

    @Override
    public void onPlaybackChanged(PlaybackEvent status)
    {
        SwingUtilities.invokeLater(() -> {
            switch (status.getInfo().getStatus())
            {
            case PLAYING -> this.playButton.setIcon(PAUSE_ICON);
            case PAUSED, STOPPED, EMPTY -> this.playButton.setIcon(PLAY_ICON);
            }
            this.progress.setValue((int) status.getInfo().getPlayTime());
            this.progress.setMaximum((int) (status.getInfo()
                                                  .getSong().duration / 1000));
        });
    }
}
