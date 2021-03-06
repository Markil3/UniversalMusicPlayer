package edu.regis.universeplayer.player;

import edu.regis.universeplayer.AbstractTask;
import edu.regis.universeplayer.PlaybackListener;
import edu.regis.universeplayer.PlaybackStatus;
import edu.regis.universeplayer.browserCommands.CommandConfirmation;
import edu.regis.universeplayer.browserCommands.QueryFuture;
import edu.regis.universeplayer.data.InternetSong;
import edu.regis.universeplayer.data.LocalSong;
import edu.regis.universeplayer.data.PlaybackEvent;
import edu.regis.universeplayer.data.Song;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

/**
 * The PlayerManager serves as the central access point for playing songs of any
 * type.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class PlayerManager implements PlaybackListener
{
    private static final Logger logger =
            LoggerFactory.getLogger(PlayerManager.class);
    private static PlayerManager INSTANCE;
    private LinkedList<PlaybackListener> listeners = new LinkedList<>();

    public static PlayerManager getPlayers()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new PlayerManager();
        }
        return INSTANCE;
    }

    /**
     * Links to other players.
     */
    private final HashMap<Class<? extends Song>, Player> players =
            new HashMap<>();

    /**
     * A reference to the player currently playing a song.
     */
    private Player currentPlayer;

    /**
     * A reference to the song currently being played.
     */
    private Song currentSong;

    public PlayerManager()
    {
        this.initPlayers();
    }

    /**
     * Obtains a song player that can play the provided song.
     *
     * @param song - The song to play.
     * @return A compatible player, or null if none is found.
     */
    public Player<?> getCompatiblePlayer(Song song)
    {
        if (song == null)
        {
            return null;
        }
        Player<?> player = this.players.get(song.getClass());
        if (player == null)
        {
            for (Class<? extends Song> songClass : this.players.keySet())
            {
                if (songClass.isAssignableFrom(song.getClass()))
                {
                    player = this.players.get(songClass);
                    break;
                }
            }
        }
        return player;
    }

    /**
     * Initializes the default players.
     */
    private void initPlayers()
    {
        this.initPlayer(LocalSong.class, new LocalPlayer());
        this.initPlayer(InternetSong.class, new BrowserPlayer());
    }

    private <T extends Song> void initPlayer(Class<T> songType,
                                             Player<T> player)
    {
        boolean playerRegistered = this.players.containsValue(player);
        this.players.put(songType, player);
        if (!playerRegistered)
        {
            player.addPlaybackListener(this);
        }
    }

    /**
     * Performs the necessary shutdown procedures for all players.
     */
    public void shutdownPlayers()
    {
        this.players.values().forEach(Player::close);
    }

    /**
     * Obtains the active song player.
     *
     * @return - The player currently playing a song.
     */
    protected Player getCurrentPlayer()
    {
        return this.currentPlayer;
    }

    /**
     * Obtains the active song.
     *
     * @return - The song currently playing.
     */
    public Song getCurrentSong()
    {
        return this.currentSong;
    }

    /**
     * Creates a QueryFuture response for if no player is found.
     *
     * @param message - The message to send.
     * @return A new future to return.
     */
    private <T> QueryFuture<T> getNullPlayer(String message, T returnValue)
    {
        return new QueryFuture<>()
        {
            @Override
            public CommandConfirmation getConfirmation() throws CancellationException
            {
                if (returnValue instanceof Throwable)
                {
                    return new CommandConfirmation((Throwable) returnValue);
                }
                else
                {
                    return new CommandConfirmation(message);
                }
            }

            @Override
            public CommandConfirmation getConfirmation(long timeout, TimeUnit unit)
            {
                return this.getConfirmation();
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning)
            {
                return false;
            }

            @Override
            public boolean isCancelled()
            {
                return false;
            }

            @Override
            public boolean isDone()
            {
                return true;
            }

            @Override
            public T get()
            {
                return returnValue;
            }

            @Override
            public T get(long timeout, TimeUnit unit)
            {
                return this.get();
            }
        };
    }

    /**
     * Creates a QueryFuture response for if no player is found.
     *
     * @param message - The error to send.
     * @return A new future to return.
     */
    private <T> QueryFuture<T> getNullPlayer(Throwable message, T returnValue)
    {
        return new QueryFuture<>()
        {
            @Override
            public CommandConfirmation getConfirmation() throws CancellationException
            {
                return new CommandConfirmation(message);
            }

            @Override
            public CommandConfirmation getConfirmation(long timeout, TimeUnit unit)
            {
                return this.getConfirmation();
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning)
            {
                return false;
            }

            @Override
            public boolean isCancelled()
            {
                return false;
            }

            @Override
            public boolean isDone()
            {
                return true;
            }

            @Override
            public T get()
            {
                return returnValue;
            }

            @Override
            public T get(long timeout, TimeUnit unit)
            {
                return this.get();
            }
        };
    }

    /**
     * Loads up a requested song and immedietally begins playback.
     *
     * @param song - The song to load.
     * @return The task that handles the request.
     */
    public ForkJoinTask<Void> playSong(Song song)
    {
        return ForkJoinPool.commonPool().submit(new AbstractTask<Void>()
        {
            @Override
            protected boolean exec()
            {
                if (currentSong != null)
                {
                    currentPlayer.stopSong().join();
                }
                currentSong = null;
                currentPlayer = getCompatiblePlayer(song);
                if (currentPlayer == null)
                {
                    throw new IllegalArgumentException("Unknown song type " + song
                            .getClass());
                }
                currentSong = song;
                currentPlayer.loadSong(song).join();
                return true;
            }
        });
    }

    /**
     * Obtains the playback status of the current player.
     *
     * @return A task containing the status, or EMPTY if no player is being
     * used.
     */
    public ForkJoinTask<PlaybackStatus> getStatus()
    {
        if (this.currentPlayer != null)
        {
            return this.currentPlayer.getStatus();
        }
        /*
         * If we have no player, then we return EMPTY.
         */
        return ForkJoinPool.commonPool().submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                complete(PlaybackStatus.EMPTY);
                return true;
            }
        });
    }

    /**
     * Seeks to the specified time stamp.
     *
     * @param time - The time to seek to, in seconds.
     * @return The task running this task.
     */
    public ForkJoinTask<Void> seek(float time)
    {
        if (this.currentPlayer != null)
        {
            return this.currentPlayer.seek(time);
        }
        return ForkJoinPool.commonPool().submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                return true;
            }
        });
    }

    public ForkJoinTask<Void> play()
    {
        if (this.currentPlayer != null && this.currentSong != null)
        {
            return this.currentPlayer.play();
        }
        return ForkJoinPool.commonPool().submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                return true;
            }
        });
    }

    public ForkJoinTask<Void> pause()
    {
        if (this.currentPlayer != null && this.currentSong != null)
        {
            return this.currentPlayer.pause();
        }
        return ForkJoinPool.commonPool().submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                return true;
            }
        });
    }

    public ForkJoinTask<Void> toggle()
    {
        if (this.currentPlayer != null && this.currentSong != null)
        {
            return this.currentPlayer.togglePlayback();
        }
        return ForkJoinPool.commonPool().submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                return true;
            }
        });
    }

    public ForkJoinTask<Void> stopSong()
    {
        if (this.currentPlayer != null && this.currentSong != null)
        {
            this.currentSong = null;
            return this.currentPlayer.stopSong();
        }
        return ForkJoinPool.commonPool().submit(new AbstractTask<>()
        {
            @Override
            protected boolean exec()
            {
                return true;
            }
        });
    }

    public void addPlaybackListener(PlaybackListener listener)
    {
        if (!this.hasPlaybackListener(listener))
        {
            this.listeners.add(listener);
        }
    }

    public boolean hasPlaybackListener(PlaybackListener listener)
    {
        return this.listeners.contains(listener);
    }

    public void removePlaybackListener(PlaybackListener listener)
    {
        this.listeners.remove(listener);
    }

    /**
     * Called when playback is changed.
     *
     * @param status - The playback status.
     */
    @Override
    public void onPlaybackChanged(PlaybackEvent status)
    {
        if (status.getSource() == this.currentPlayer)
        {
            for (PlaybackListener l : this.listeners)
            {
                l.onPlaybackChanged(status);
            }
        }
    }

    /**
     * Sends an error to the browser player.
     *
     * @param forward - Whether the error should be thrown in a foreground
     *                script or a background script.
     * @return The command future.
     */
    public ForkJoinTask<Void> throwError(boolean forward)
    {
        BrowserPlayer player =
                (BrowserPlayer) this.players.get(InternetSong.class);
        if (player == null)
        {
            return new AbstractTask<>()
            {
                @Override
                protected boolean exec()
                {
                    completeExceptionally(new NullPointerException("Couldn't " +
                            "find internet player"));
                    return false;
                }
            };
        }
        return player.throwError(forward);
    }
}
