package edu.regis.universeplayer.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JOptionPane;

import edu.regis.universeplayer.PlaybackListener;
import edu.regis.universeplayer.PlaybackStatus;
import edu.regis.universeplayer.browserCommands.CommandConfirmation;
import edu.regis.universeplayer.browserCommands.QueryFuture;
import edu.regis.universeplayer.data.InternetSong;
import edu.regis.universeplayer.data.LocalSong;
import edu.regis.universeplayer.data.PlaybackEvent;
import edu.regis.universeplayer.data.Queue;
import edu.regis.universeplayer.data.Song;
import edu.regis.universeplayer.gui.Interface;

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
        return new QueryFuture<T>()
        {
            @Override
            public CommandConfirmation getConfirmation() throws CancellationException, ExecutionException, InterruptedException
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
            public CommandConfirmation getConfirmation(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
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
            public T get() throws InterruptedException, ExecutionException
            {
                return returnValue;
            }

            @Override
            public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
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
        return new QueryFuture<T>()
        {
            @Override
            public CommandConfirmation getConfirmation() throws CancellationException, ExecutionException, InterruptedException
            {
                return new CommandConfirmation(message);
            }

            @Override
            public CommandConfirmation getConfirmation(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
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
            public T get() throws InterruptedException, ExecutionException
            {
                return returnValue;
            }

            @Override
            public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
            {
                return this.get();
            }
        };
    }

    public QueryFuture<Void> playSong(Song song)
    {
        if (this.currentSong != null)
        {
            this.currentPlayer.stopSong();
        }
        this.currentSong = null;
        this.currentPlayer = this.getCompatiblePlayer(song);
        if (this.currentPlayer == null)
        {
            throw new IllegalArgumentException("Unknown song type " + song
                    .getClass());
        }
        this.currentSong = song;
        return this.currentPlayer.loadSong(song);
    }

    public QueryFuture<PlaybackStatus> getStatus()
    {
        if (this.currentPlayer != null)
        {
            return this.currentPlayer.getStatus();
        }
        return this.getNullPlayer("Command successful", PlaybackStatus.EMPTY);
    }

    public QueryFuture<Void> seek(float time)
    {
        if (this.currentPlayer != null)
        {
            return this.currentPlayer.seek(time);
        }
        return null;
    }

    public QueryFuture<Void> play()
    {
        if (this.currentPlayer != null && this.currentSong != null)
        {
            return this.currentPlayer.play();
        }
        return null;
    }

    public QueryFuture<Void> pause()
    {
        if (this.currentPlayer != null && this.currentSong != null)
        {
            return this.currentPlayer.pause();
        }
        return null;
    }

    public QueryFuture<Void> toggle()
    {
        if (this.currentPlayer != null && this.currentSong != null)
        {
            return this.currentPlayer.togglePlayback();
        }
        return null;
    }

    public QueryFuture<Void> stopSong()
    {
        if (this.currentPlayer != null && this.currentSong != null)
        {
            this.currentSong = null;
            return this.currentPlayer.stopSong();
        }
        return null;
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
     * @param forward - Whether the error should be thrown in a foreground
     *                script or a background script.
     * @return The command future.
     */
    public QueryFuture<Void> throwError(boolean forward)
    {
        BrowserPlayer player =
                (BrowserPlayer) this.players.get(InternetSong.class);
        if (player == null)
        {
            return this.getNullPlayer(new NullPointerException(
                    "No browser found"), null);
        }
        return player.throwError(forward);
    }
}
