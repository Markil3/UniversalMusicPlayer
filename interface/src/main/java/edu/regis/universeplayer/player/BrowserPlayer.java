/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import edu.regis.universeplayer.AbstractTask;
import edu.regis.universeplayer.NumberPing;
import edu.regis.universeplayer.PlaybackInfo;
import edu.regis.universeplayer.PlaybackListener;
import edu.regis.universeplayer.PlaybackStatus;
import edu.regis.universeplayer.browser.Browser;
import edu.regis.universeplayer.browserCommands.*;
import edu.regis.universeplayer.data.InternetSong;
import edu.regis.universeplayer.data.PlaybackEvent;
import edu.regis.universeplayer.data.Song;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.*;

/**
 * This serves as a central point for controlling the browser process.
 *
 * @author William Hubbard
 * @since 0.1
 */
public class BrowserPlayer implements Player<InternetSong>, UpdateListener
{
    private static final Logger logger = LoggerFactory
            .getLogger(BrowserPlayer.class);

    /**
     * The player instance. This should only be used for debugging..
     */
    private static BrowserPlayer INSTANCE;

    /**
     * Obtains the player instance. This should only be used for debugging.
     */
    public static BrowserPlayer getInstance()
    {
        return INSTANCE;
    }

    private final ForkJoinPool service = new ForkJoinPool();
    private final LinkedList<PlaybackListener> listeners = new LinkedList<>();
    private boolean error = false;

    private InternetSong currentSong;

    private Browser browserRef = null;

    private Browser getBrowser()
    {
        if (browserRef == null && !this.error)
        {
            if (Browser.getInstance() == null)
            {
                try
                {
                    Browser.waitInstance();
                }
                catch (InterruptedException e)
                {
                    logger.error("Interrupted while waiting for browser to initialize.", e);
                }
            }
            if (Browser.getInstance() != null)
            {
                Browser.getInstance().addUpdateListener(this);
                browserRef = Browser.getInstance();
            }
        }
        return browserRef;
    }

    public BrowserPlayer()
    {
        INSTANCE = this;
        Thread browserThread = new Thread(() -> {
            try
            {
                this.browserRef = Browser.createBrowser();
                this.browserRef.run();
            }
            catch (IOException | InterruptedException e)
            {
                logger.error("Could not initialize browser", e);
                this.error = true;
                Browser.notifyAllInstance();
            }
        }, "BrowserThread");
        browserThread.start();
    }

    @Override
    public Song getCurrentSong()
    {
        return this.currentSong;
    }

    /**
     * Obtains data on a specified song.
     *
     * @param url - The url to get the data from.
     * @return A confirmation of command success.
     */
    public ForkJoinTask<InternetSong> getSongData(URL url)
    {
        return this.service.submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                try
                {
                    Future<?> command =
                            getBrowser()
                                    .sendObject(new QuerySongData(url));
                    CommandReturn<InternetSong> returnOb =
                            (CommandReturn<InternetSong>) command
                            .get();
                    if (!returnOb.getConfirmation().wasSuccessful())
                    {
                        this.completeExceptionally(returnOb.getConfirmation()
                                                           .getError());
                        return false;
                    }
                    else
                    {
                        this.complete(returnOb.getReturnValue());
                        return true;
                    }
                }
                catch (IOException | InterruptedException | ExecutionException e)
                {
                    this.completeExceptionally(e);
                    return false;
                }
            }
        });
    }

    @Override
    public ForkJoinTask<Void> loadSong(InternetSong song)
    {
        return this.service.submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                try
                {
                    Future<?> command =
                            getBrowser()
                                    .sendObject(new CommandLoadSong(song.location));
                    CommandReturn<Boolean> returnOb = (CommandReturn<Boolean>) command
                            .get();
                    if (!returnOb.getConfirmation().wasSuccessful())
                    {
                        this.completeExceptionally(returnOb.getConfirmation()
                                                           .getError());
                        return false;
                    }
                    else
                    {
                        return true;
                    }
                }
                catch (IOException | InterruptedException | ExecutionException e)
                {
                    this.completeExceptionally(e);
                    return false;
                }
            }
        });
    }

    /**
     * Tells the browser process to shut down.
     */
    @Override
    public ForkJoinTask<Void> close()
    {
        return this.service.submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                try
                {
                    Future<?> command =
                            getBrowser()
                                    .sendObject(new CommandQuit());
                    CommandReturn<Boolean> returnOb = (CommandReturn<Boolean>) command
                            .get();
                    if (!returnOb.getConfirmation().wasSuccessful())
                    {
                        this.completeExceptionally(returnOb.getConfirmation()
                                                           .getError());
                        return false;
                    }
                    else
                    {
                        return true;
                    }
                }
                catch (IOException | InterruptedException | ExecutionException e)
                {
                    this.completeExceptionally(e);
                    return false;
                }
            }
        });
    }

    /**
     * Adds a listener for playback status updates.
     *
     * @param listener - The listener to add.
     */
    @Override
    public void addPlaybackListener(PlaybackListener listener)
    {
        this.listeners.add(listener);
    }

    /**
     * Checks to see if a listener has been added.
     *
     * @param listener - The listener to look for.
     * @return True if the provided listener has been added, false otherwise.
     */
    @Override
    public boolean hasPlaybackListener(PlaybackListener listener)
    {
        return this.listeners.contains(listener);
    }

    /**
     * Removes a listener for playback status updates.
     *
     * @param listener - The listener to remove.
     */
    @Override
    public void removePlaybackListener(PlaybackListener listener)
    {
        this.listeners.remove(listener);
    }

    @Override
    public ForkJoinTask<Void> play()
    {
        return this.service.submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                try
                {
                    Future<?> command =
                            getBrowser()
                                    .sendObject(new CommandSetPlayback(CommandSetPlayback.Playback.PLAY));
                    CommandReturn<Boolean> returnOb = (CommandReturn<Boolean>) command
                            .get();
                    if (!returnOb.getConfirmation().wasSuccessful())
                    {
                        this.completeExceptionally(returnOb.getConfirmation()
                                                           .getError());
                        return true;
                    }
                    else
                    {
                        return true;
                    }
                }
                catch (IOException | InterruptedException | ExecutionException e)
                {
                    this.completeExceptionally(e);
                    return false;
                }
            }
        });
    }

    @Override
    public ForkJoinTask<Void> pause()
    {
        return this.service.submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                try
                {
                    Future<?> command =
                            getBrowser()
                                    .sendObject(new CommandSetPlayback(CommandSetPlayback.Playback.PAUSE));
                    CommandReturn<Boolean> returnOb = (CommandReturn<Boolean>) command
                            .get();
                    if (!returnOb.getConfirmation().wasSuccessful())
                    {
                        this.completeExceptionally(returnOb.getConfirmation()
                                                           .getError());
                        return false;
                    }
                    else
                    {
                        return true;
                    }
                }
                catch (IOException | InterruptedException | ExecutionException e)
                {
                    this.completeExceptionally(e);
                    return false;
                }
            }
        });
    }

    @Override
    public ForkJoinTask<Void> togglePlayback()
    {
        return this.service.submit(new AbstractTask<Void>()
        {
            @Override
            protected boolean exec()
            {
                try
                {
                    Future<?> command = getBrowser()
                            .sendObject(new QueryStatus());
                    CommandReturn<String> returnOb =
                            (CommandReturn<String>) command.get();
                    CommandReturn<?> confirmation;
                    if (!returnOb.getConfirmation().wasSuccessful())
                    {
                        this.completeExceptionally(returnOb.getConfirmation()
                                                           .getError());
                        return false;
                    }
                    else
                    {
                        switch (PlaybackStatus
                                .valueOf(returnOb.getReturnValue()))
                        {
                        case PLAYING -> command =
                                getBrowser()
                                        .sendObject(new CommandSetPlayback(CommandSetPlayback.Playback.PAUSE));
                        case PAUSED, STOPPED, FINISHED -> command = getBrowser()
                                .sendObject(new CommandSetPlayback(CommandSetPlayback.Playback.PLAY));
                        }
                        confirmation =
                                (CommandReturn<?>) command.get();
                        if (!confirmation.getConfirmation().wasSuccessful())
                        {
                            this.completeExceptionally(returnOb
                                    .getConfirmation().getError());
                            return false;
                        }
                        else
                        {
                            return true;
                        }
                    }
                }
                catch (IOException | InterruptedException | ExecutionException e)
                {
                    this.completeExceptionally(e);
                    return false;
                }
            }
        });
    }

    /**
     * Stops playback of the current song.
     */
    @Override
    public ForkJoinTask<Void> stopSong()
    {
        return this.service.submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                try
                {
                    Future<?> command =
                            getBrowser()
                                    .sendObject(new CommandLoadSong((URL) null));
                    CommandReturn<Boolean> returnOb = (CommandReturn<Boolean>) command
                            .get();
                    if (!returnOb.getConfirmation().wasSuccessful())
                    {
                        this.completeExceptionally(returnOb.getConfirmation()
                                                           .getError());
                        return false;
                    }
                    else
                    {
                        return true;
                    }
                }
                catch (IOException | InterruptedException | ExecutionException e)
                {
                    this.completeExceptionally(e);
                    return false;
                }
            }
        });
    }

    @Override
    public ForkJoinTask<Void> seek(float time)
    {
        return this.service.submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                try
                {
                    Future<?> command =
                            getBrowser()
                                    .sendObject(new CommandSeek(time));
                    CommandReturn<Boolean> returnOb = (CommandReturn<Boolean>) command
                            .get();
                    if (!returnOb.getConfirmation().wasSuccessful())
                    {
                        this.completeExceptionally(returnOb.getConfirmation()
                                                           .getError());
                        return false;
                    }
                    else
                    {
                        return true;
                    }
                }
                catch (IOException | InterruptedException | ExecutionException e)
                {
                    this.completeExceptionally(e);
                    return false;
                }
            }
        });
    }

    /**
     * Obtains the player's current playback status.
     *
     * @return A future for the request.
     */
    @Override
    public ForkJoinTask<PlaybackStatus> getStatus()
    {
        return this.service.submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                try
                {
                    Future<?> command = getBrowser()
                            .sendObject(new QueryStatus());
                    CommandReturn<String> returnOb =
                            (CommandReturn<String>) command.get();
                    if (!returnOb.getConfirmation().wasSuccessful())
                    {
                        this.completeExceptionally(returnOb.getConfirmation()
                                                           .getError());
                        return false;
                    }
                    else
                    {
                        this.complete(PlaybackStatus
                                .valueOf(returnOb.getReturnValue()));
                        return true;
                    }
                }
                catch (IOException | InterruptedException | ExecutionException e)
                {
                    this.completeExceptionally(e);
                    return false;
                }
            }
        });
    }

    @Override
    public ForkJoinTask<Float> getCurrentTime()
    {
        return null;
    }

    @Override
    public ForkJoinTask<Float> getLength()
    {
        return null;
    }

    /**
     * Causes the browser to throw an error
     *
     * @param forward - Whether the error should be thrown from the foreground
     *                script or the background script.
     * @return The return value containing error details.
     */
    public ForkJoinTask<Void> throwError(boolean forward)
    {
        return this.service.submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                try
                {
                    Future<?> command =
                            getBrowser()
                                    .sendObject(new CommandError(forward));
                    CommandReturn<Boolean> returnOb = (CommandReturn<Boolean>) command
                            .get();
                    if (!returnOb.getConfirmation().wasSuccessful())
                    {
                        this.completeExceptionally(returnOb.getConfirmation()
                                                           .getError());
                        return false;
                    }
                    else
                    {
                        return true;
                    }
                }
                catch (IOException | InterruptedException | ExecutionException e)
                {
                    this.completeExceptionally(e);
                    return false;
                }
            }
        });
    }

    /**
     * Pings the browser background.
     *
     * @param i - The number to send.
     * @return A future hopefully returning the same value.
     */
    public ForkJoinTask<Double> ping(double i)
    {
        return this.ping(null, i);
    }

    /**
     * Pings the browser foreground.
     *
     * @param url - The url to open.
     * @param i   - The number to send.
     * @return A future hopefully returning the same value.
     */
    public ForkJoinTask<Double> ping(URL url, double i)
    {
        return this.service.submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                try
                {
                    Future<?> command = getBrowser()
                            .sendObject(new NumberPing(url, i));
                    CommandReturn<Number> returnOb =
                            (CommandReturn<Number>) command.get();
                    if (!returnOb.getConfirmation().wasSuccessful())
                    {
                        this.completeExceptionally(returnOb.getConfirmation()
                                                           .getError());
                        return false;
                    }
                    else
                    {
                        this.complete(returnOb.getReturnValue().doubleValue());
                        return true;
                    }
                }
                catch (IOException | InterruptedException | ExecutionException e)
                {
                    this.completeExceptionally(e);
                    return false;
                }
            }
        });
    }

    @Override
    public void onUpdate(Object object, MessageRunner runner)
    {
        PlaybackEvent status;
        if (object instanceof PlaybackInfo)
        {
            status = new PlaybackEvent(this, (PlaybackInfo) object);
            logger.info("Internet playback {}", status.getInfo());
            this.listeners.forEach(l -> l.onPlaybackChanged(status));
        }
    }
}
