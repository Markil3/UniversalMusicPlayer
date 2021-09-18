/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

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
        });
        browserThread.start();
    }

    @Override
    public Song getCurrentSong()
    {
        return this.currentSong;
    }

    @Override
    public QueryFuture<Void> loadSong(InternetSong song)
    {
        try
        {
            return new ForwardedFuture(getBrowser()
                    .sendObject(new CommandLoadSong(song.location)));
        }
        catch (IOException e)
        {
            logger.error("Could not send message", e);
            return null;
        }
    }

    /**
     * Tells the browser process to shut down.
     */
    @Override
    public QueryFuture<Void> close()
    {
        try
        {
            return (QueryFuture<Void>) new ForwardedFuture(getBrowser()
                    .sendObject(new CommandQuit()));
        }
        catch (IOException e)
        {
            logger.error("Could not send message", e);
            return null;
        }
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
    public QueryFuture<Void> play()
    {
        try
        {
            return new ForwardedFuture(getBrowser()
                    .sendObject(new CommandSetPlayback(CommandSetPlayback.Playback.PLAY)));
        }
        catch (IOException e)
        {
            logger.error("Could not send message", e);
            return null;
        }
    }

    @Override
    public QueryFuture<Void> pause()
    {
        try
        {
            return new ForwardedFuture(getBrowser()
                    .sendObject(new CommandSetPlayback(CommandSetPlayback.Playback.PAUSE)));
        }
        catch (IOException e)
        {
            logger.error("Could not send message", e);
            return null;
        }
    }

    @Override
    public QueryFuture<Void> togglePlayback()
    {
        try
        {
            QueryFuture<PlaybackStatus> future = this.getStatus();
            switch (future.get())
            {
            case PLAYING -> {
                return new ForwardedFuture(getBrowser()
                        .sendObject(new CommandSetPlayback(CommandSetPlayback.Playback.PAUSE)));
            }
            case PAUSED, STOPPED, FINISHED -> {
                return new ForwardedFuture(getBrowser()
                        .sendObject(new CommandSetPlayback(CommandSetPlayback.Playback.PLAY)));
            }
            }
            return null;
        }
        catch (IOException | InterruptedException | ExecutionException e)
        {
            logger.error("Could not send message", e);
            return null;
        }
    }

    /**
     * Stops playback of the current song.
     */
    @Override
    public QueryFuture<Void> stopSong()
    {
        try
        {
            return new ForwardedFuture(getBrowser()
                    .sendObject(new CommandLoadSong((URL) null)));
        }
        catch (IOException e)
        {
            logger.error("Could not send message", e);
            return null;
        }
    }

    @Override
    public QueryFuture<Void> seek(float time)
    {
        try
        {
            return new ForwardedFuture(getBrowser()
                    .sendObject(new CommandSeek(time)));
        }
        catch (IOException e)
        {
            logger.error("Could not send message", e);
            return null;
        }
    }

    /**
     * Obtains the player's current playback status.
     *
     * @return A future for the request.
     */
    @Override
    public QueryFuture<PlaybackStatus> getStatus()
    {
        try
        {
            Future future = getBrowser().sendObject(new QueryStatus());
            return new QueryFuture<>()
            {

                private CommandReturn<String> getVal() throws ExecutionException, InterruptedException
                {
                    return ((CommandReturn<String>) future.get());
                }

                private CommandReturn<String> getVal(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException
                {
                    return ((CommandReturn<String>) future.get(timeout, unit));
                }

                @Override
                public CommandConfirmation getConfirmation() throws CancellationException, ExecutionException, InterruptedException
                {
                    return this.getVal().getConfirmation();
                }

                @Override
                public CommandConfirmation getConfirmation(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException
                {
                    return this.getVal(timeout, unit).getConfirmation();
                }

                @Override
                public boolean cancel(boolean mayInterruptIfRunning)
                {
                    return future.cancel(mayInterruptIfRunning);
                }

                @Override
                public boolean isCancelled()
                {
                    return future.isCancelled();
                }

                @Override
                public boolean isDone()
                {
                    return future.isDone();
                }

                @Override
                public PlaybackStatus get() throws InterruptedException, ExecutionException
                {
                    String value = getVal().getReturnValue();
                    return PlaybackStatus.valueOf(value);
                }

                @Override
                public PlaybackStatus get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
                {
                    String value = getVal(timeout, unit).getReturnValue();
                    return PlaybackStatus.valueOf(value);
                }
            };
        }
        catch (IOException e)
        {
            logger.error("Could not send message", e);
            return null;
        }
    }

    @Override
    public QueryFuture<Float> getCurrentTime()
    {
        return null;
    }

    @Override
    public QueryFuture<Float> getLength()
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
    public QueryFuture<Void> throwError(boolean forward)
    {
        try
        {
            return new ForwardedFuture(getBrowser()
                    .sendObject(new CommandError(forward)));
        }
        catch (IOException e)
        {
            logger.error("Could not send message", e);
            return null;
        }
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

    private class ForwardedFuture<T> implements QueryFuture<T>
    {
        private final Future<T> future;

        ForwardedFuture(Future<T> future)
        {
            this.future = future;
        }

        private CommandReturn<T> getVal() throws ExecutionException, InterruptedException
        {
            return ((CommandReturn<T>) this.future.get());
        }

        private CommandReturn<T> getVal(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException
        {
            return ((CommandReturn<T>) this.future.get(timeout, unit));
        }

        @Override
        public CommandConfirmation getConfirmation() throws CancellationException, ExecutionException, InterruptedException
        {
            return this.getVal().getConfirmation();
        }

        @Override
        public CommandConfirmation getConfirmation(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
        {
            return this.getVal(timeout, unit).getConfirmation();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            return this.future.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled()
        {
            return this.future.isCancelled();
        }

        @Override
        public boolean isDone()
        {
            return this.future.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException
        {
            return this.getVal().getReturnValue();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
        {
            return this.getVal(timeout, unit).getReturnValue();
        }
    }
}
