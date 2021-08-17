/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */
package edu.regis.universeplayer.localPlayer;

import com.intervigil.wave.WaveReader;
import edu.regis.universeplayer.PlaybackInfo;
import edu.regis.universeplayer.PlaybackListener;
import edu.regis.universeplayer.PlaybackStatus;
import edu.regis.universeplayer.Player;
import edu.regis.universeplayer.browserCommands.CommandConfirmation;
import edu.regis.universeplayer.browserCommands.QueryFuture;
import edu.regis.universeplayer.data.LocalSong;
import edu.regis.universeplayer.data.PlaybackEvent;
import edu.regis.universeplayer.data.Song;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.media.TrackType;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.base.StatusApi;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.*;

/**
 * This allows for the control of playback of files on the local file system
 *
 * @author William Hubbard
 * @version 0.1
 */
public class LocalPlayer implements Player<LocalSong>, MediaPlayerEventListener
{
    private static final Logger logger = LoggerFactory.getLogger(LocalPlayer.class);

    private final MediaPlayerFactory playerFactory;
    private final AudioPlayerComponent player;

    private final LinkedList<PlaybackListener> listeners = new LinkedList<>();

    private int currentId;

    private LocalSong currentSong;
    private AudioFile currentFile;

    private final ExecutorService service = Executors.newSingleThreadExecutor();

    public LocalPlayer()
    {
        this.playerFactory = new MediaPlayerFactory();
        this.player = new AudioPlayerComponent();
        this.player.mediaPlayer().events().addMediaPlayerEventListener(this);
    }

    /**
     * Sets the file currently being used.
     *
     * @param file - The current audio file.
     */
    public void setCurrentFile(AudioFile file)
    {
        WaveReader header = file.getHeader();
        this.currentFile = file;
        // TODO - Ensure that bytes per sample and bits per sample don't bother things
    }

    /**
     * Saves the current file as a WAVE file somewhere else. This method is meant for testing only.
     *
     * @param file - The file to save to.
     */
    public void saveJava(String file)
    {
        try (FileOutputStream out = new FileOutputStream(file))
        {
            out.write(this.getAudioFile().getByteStream());
            byte[] buffer = new byte[256];
            int read = this.getAudioFile().read(buffer);
            while (read > 0)
            {
                out.write(buffer, 0, read);
                read = this.getAudioFile().read(buffer);
            }
        }
        catch (IOException e)
        {
            logger.error("Could not save WAVE file", e);
        }
    }

    /**
     * Obtains the current file.
     *
     * @return The current audio file we are working with.
     */
    public AudioFile getAudioFile()
    {
        return this.currentFile;
    }

    @Override
    public Song getCurrentSong()
    {
        return this.currentSong;
    }

    @Override
    public QueryFuture<Void> loadSong(LocalSong song)
    {
        if (this.currentSong != null)
        {
            this.stopSong();
        }
        this.currentSong = song;
        this.player.mediaPlayer().media().play(song.file.getAbsolutePath());
        return new NullFuture<>();
    }

    @Override
    public QueryFuture<Void> play()
    {
        this.player.mediaPlayer().submit((WrappedRunnable) () -> this.player.mediaPlayer().controls().play());
        return new NullFuture<>();
    }

    @Override
    public QueryFuture<Void> pause()
    {
        this.player.mediaPlayer().submit((WrappedRunnable) () -> this.player.mediaPlayer().controls().pause());
        return new NullFuture<>();
    }

    @Override
    public QueryFuture<Void> togglePlayback()
    {
        if (this.player.mediaPlayer().status().isPlaying())
        {
            return this.pause();
        }
        else
        {
            return this.play();
        }
    }

    @Override
    public QueryFuture<Void> stopSong()
    {
        this.player.mediaPlayer().submit((WrappedRunnable) () -> this.player.mediaPlayer().controls().stop());
        return new NullFuture<>();
    }

    @Override
    public QueryFuture<Void> seek(float time)
    {
        this.player.mediaPlayer().submit((WrappedRunnable) () -> this.player.mediaPlayer().controls()
                                                          .setTime((long) (time * 1000)));
        return new NullFuture<>();
    }

    @Override
    public QueryFuture<PlaybackStatus> getStatus()
    {
        PlaybackStatus returnStatus;
        StatusApi status = this.player.mediaPlayer().status();
        switch (status.state())
        {
        case NOTHING_SPECIAL -> returnStatus = PlaybackStatus.EMPTY;
        case PLAYING -> returnStatus = PlaybackStatus.PLAYING;
        case PAUSED -> returnStatus = PlaybackStatus.PAUSED;
        default -> returnStatus = PlaybackStatus.STOPPED;
        }
        return new NullFuture<>(returnStatus);
    }

    @Override
    public QueryFuture<Float> getCurrentTime()
    {
        return new NullFuture<>(this.player.mediaPlayer().status().time() / 1000F);
    }

    @Override
    public QueryFuture<Float> getLength()
    {
        return new NullFuture<>(this.player.mediaPlayer().status().length() / 1000F);
    }

    @Override
    public QueryFuture<Void> close()
    {
        this.player.release();
        return new NullFuture<>();
    }

    @Override
    public void addPlaybackListener(PlaybackListener listener)
    {
        if (!this.hasPlaybackListener(listener))
        {
            this.listeners.add(listener);
        }
    }

    @Override
    public boolean hasPlaybackListener(PlaybackListener listener)
    {
        return this.listeners.contains(listener);
    }

    @Override
    public void removePlaybackListener(PlaybackListener listener)
    {
        this.listeners.remove(listener);
    }

    /**
     * Obtains an input stream for the requested file.
     *
     * @param file - The file to read
     * @return An raw stream for the file
     * @throws FileNotFoundException - Thrown should the file not exist.
     * @throws IOException           - Thrown should an error occur when reading the file
     */
    public static AudioFile getAudioStream(File file) throws IOException
    {
        return new AudioFile(convertFile(file));
    }

    /**
     * Converts any audio file to a stream containing WAV audio file data (courtesy of FFMPEG).
     *
     * @param file - The file to convert
     * @return An input stream containing the file data
     * @throws FileNotFoundException - Thrown should the file not exist.
     * @throws IOException           - Thrown should an error occur when reading the file
     */
    protected static Process convertFile(File file) throws FileNotFoundException, IOException
    {
        LinkedList<String> args = new LinkedList<>();
        if (!file.isFile())
        {
            throw new FileNotFoundException("Must provide a file");
        }
        args.add("ffmpeg");
        args.add("-hide_banner");
        args.add("-loglevel");
        args.add("error");
        args.add("-y");
        args.add("-i");
        args.add(file.getAbsolutePath());
        args.add("-f");
        args.add("wav");
        args.add("pipe:1");
        return Runtime.getRuntime().exec(args.toArray(String[]::new));
    }

    public static void main(String[] args)
    {
    }

    /**
     * The media changed.
     *
     * @param mediaPlayer media player that raised the event
     * @param media       new media instance
     */
    @Override
    public void mediaChanged(MediaPlayer mediaPlayer, MediaRef media)
    {
        logger.debug("New media {} loaded", media);
    }

    /**
     * Opening the media.
     *
     * @param mediaPlayer media player that raised the event
     */
    @Override
    public void opening(MediaPlayer mediaPlayer)
    {

    }

    /**
     * Buffering media.
     *
     * @param mediaPlayer media player that raised the event
     * @param newCache    percentage complete, ranging from 0.0 to 100.0
     */
    @Override
    public void buffering(MediaPlayer mediaPlayer, float newCache)
    {

    }

    /**
     * The media started playing.
     * <p>
     * There is no guarantee that a video output has been created at this point.
     *
     * @param mediaPlayer media player that raised the event
     */
    @Override
    public void playing(MediaPlayer mediaPlayer)
    {
        logger.debug("Local player playing.");
        SwingUtilities.invokeLater(() -> this.listeners.forEach(playbackListener -> playbackListener.onPlaybackChanged(new PlaybackEvent(this, new PlaybackInfo(this.currentSong, mediaPlayer.status().time(), PlaybackStatus.PLAYING)))));
    }

    /**
     * Media paused.
     *
     * @param mediaPlayer media player that raised the event
     */
    @Override
    public void paused(MediaPlayer mediaPlayer)
    {
        logger.debug("Local player paused.");
        SwingUtilities.invokeLater(() -> this.listeners.forEach(playbackListener -> playbackListener.onPlaybackChanged(new PlaybackEvent(this, new PlaybackInfo(this.currentSong, mediaPlayer.status().time(), PlaybackStatus.PAUSED)))));
    }

    /**
     * Media stopped.
     * <p>
     * A stopped event may be raised under certain circumstances even if the media player is not playing (e.g. as part
     * of the associated media list player sub-item handling). Client applications must therefore be prepared to handle
     * such a situation.
     *
     * @param mediaPlayer media player that raised the event
     */
    @Override
    public void stopped(MediaPlayer mediaPlayer)
    {
        logger.debug("Local player stopped prematurely.");
        SwingUtilities.invokeLater(() -> this.listeners.forEach(playbackListener -> playbackListener.onPlaybackChanged(new PlaybackEvent(this, new PlaybackInfo(this.currentSong, mediaPlayer.status().time(), PlaybackStatus.STOPPED)))));
    }

    /**
     * Media skipped forward.
     *
     * @param mediaPlayer media player that raised the event
     */
    @Override
    public void forward(MediaPlayer mediaPlayer)
    {

    }

    /**
     * Media skipped backward.
     *
     * @param mediaPlayer media player that raised the event
     */
    @Override
    public void backward(MediaPlayer mediaPlayer)
    {

    }

    /**
     * Media finished playing (i.e. the end was reached without being stopped).
     *
     * @param mediaPlayer media player that raised the event
     */
    @Override
    public void finished(MediaPlayer mediaPlayer)
    {
        logger.debug("Local player finished.");
        SwingUtilities.invokeLater(() -> this.listeners.forEach(playbackListener -> playbackListener.onPlaybackChanged(new PlaybackEvent(this, new PlaybackInfo(this.currentSong, mediaPlayer.status().time(), PlaybackStatus.FINISHED)))));
    }

    /**
     * Media play-back time changed.
     *
     * @param mediaPlayer media player that raised the event
     * @param newTime     new time
     */
    @Override
    public void timeChanged(MediaPlayer mediaPlayer, long newTime)
    {
        SwingUtilities.invokeLater(() -> this.listeners.forEach(playbackListener -> playbackListener.onPlaybackChanged(new PlaybackEvent(this, new PlaybackInfo(this.currentSong, newTime / 1000F, PlaybackStatus.PLAYING)))));
    }

    /**
     * Media play-back position changed.
     *
     * @param mediaPlayer media player that raised the event
     * @param newPosition percentage between 0.0 and 1.0
     */
    @Override
    public void positionChanged(MediaPlayer mediaPlayer, float newPosition)
    {

    }

    /**
     * Media seekable status changed.
     *
     * @param mediaPlayer media player that raised the event
     * @param newSeekable new seekable status
     */
    @Override
    public void seekableChanged(MediaPlayer mediaPlayer, int newSeekable)
    {

    }

    /**
     * Media pausable status changed.
     *
     * @param mediaPlayer media player that raised the event
     * @param newPausable new pausable status
     */
    @Override
    public void pausableChanged(MediaPlayer mediaPlayer, int newPausable)
    {

    }

    /**
     * Media title changed.
     *
     * @param mediaPlayer media player that raised the event
     * @param newTitle    new title
     */
    @Override
    public void titleChanged(MediaPlayer mediaPlayer, int newTitle)
    {

    }

    /**
     * A snapshot was taken.
     *
     * @param mediaPlayer media player that raised the event
     * @param filename    name of the file containing the snapshot
     */
    @Override
    public void snapshotTaken(MediaPlayer mediaPlayer, String filename)
    {

    }

    /**
     * Media length changed.
     *
     * @param mediaPlayer media player that raised the event
     * @param newLength   new length (number of milliseconds)
     */
    @Override
    public void lengthChanged(MediaPlayer mediaPlayer, long newLength)
    {

    }

    /**
     * The number of video outputs changed.
     *
     * @param mediaPlayer media player that raised the event
     * @param newCount    new number of video outputs
     */
    @Override
    public void videoOutput(MediaPlayer mediaPlayer, int newCount)
    {

    }

    /**
     * Program scrambled changed.
     *
     * @param mediaPlayer  media player that raised the event
     * @param newScrambled new scrambled value
     */
    @Override
    public void scrambledChanged(MediaPlayer mediaPlayer, int newScrambled)
    {

    }

    /**
     * An elementary stream was added.
     *
     * @param mediaPlayer media player that raised the event
     * @param type        type of stream
     * @param id          identifier of stream
     */
    @Override
    public void elementaryStreamAdded(MediaPlayer mediaPlayer, TrackType type, int id)
    {

    }

    /**
     * An elementary stream was deleted.
     *
     * @param mediaPlayer media player that raised the event
     * @param type        type of stream
     * @param id          identifier of stream
     */
    @Override
    public void elementaryStreamDeleted(MediaPlayer mediaPlayer, TrackType type, int id)
    {

    }

    /**
     * An elementary stream was selected.
     *
     * @param mediaPlayer media player that raised the event
     * @param type        type of stream
     * @param id          identifier of stream
     */
    @Override
    public void elementaryStreamSelected(MediaPlayer mediaPlayer, TrackType type, int id)
    {

    }

    /**
     * The media player was corked/un-corked.
     * <p>
     * Corking/un-corking can occur e.g. when another media player (or some
     * other application) starts/stops playing media.
     *
     * @param mediaPlayer media player that raised the event
     * @param corked      <code>true</code> if corked; otherwise <code>false</code>
     */
    @Override
    public void corked(MediaPlayer mediaPlayer, boolean corked)
    {

    }

    /**
     * The audio was muted/un-muted.
     *
     * @param mediaPlayer media player that raised the event
     * @param muted       <code>true</code> if muted; otherwise <code>false</code>
     */
    @Override
    public void muted(MediaPlayer mediaPlayer, boolean muted)
    {

    }

    /**
     * The volume changed.
     *
     * @param mediaPlayer media player that raised the event
     * @param volume      new volume
     */
    @Override
    public void volumeChanged(MediaPlayer mediaPlayer, float volume)
    {

    }

    /**
     * The audio device changed.
     *
     * @param mediaPlayer media player that raised the event
     * @param audioDevice new audio device
     */
    @Override
    public void audioDeviceChanged(MediaPlayer mediaPlayer, String audioDevice)
    {

    }

    /**
     * The chapter changed.
     *
     * @param mediaPlayer media player that raised the event
     * @param newChapter  new chapter
     */
    @Override
    public void chapterChanged(MediaPlayer mediaPlayer, int newChapter)
    {

    }

    /**
     * An error occurred.
     *
     * @param mediaPlayer media player that raised the event
     */
    @Override
    public void error(MediaPlayer mediaPlayer)
    {
        logger.error("Local player error");

    }

    /**
     * Media player is ready (to enable features like logo and marquee) after
     * the media has started playing.
     * <p>
     * The implementation will fire this event once on receipt of the first
     * native position-changed event with a position value greater than zero.
     * <p>
     * The event will be fired again if the media is played again after a native
     * stopped or finished event is received.
     * <p>
     * Waiting for this event may be more reliable than using {@link #playing(MediaPlayer)}
     * or {@link #videoOutput(MediaPlayer, int)} in some cases (logo and marquee
     * already mentioned, also setting audio tracks, sub-title tracks and so on).
     *
     * @param mediaPlayer media player that raised the event
     */
    @Override
    public void mediaPlayerReady(MediaPlayer mediaPlayer)
    {
        logger.debug("Local player ready.");
    }

    private static class WrappedFuture<T> implements QueryFuture<T>
    {
        private final Future<T> runnable;

        WrappedFuture(Future<T> runnable)
        {
            this.runnable = runnable;
        }

        @Override
        public CommandConfirmation getConfirmation() throws CancellationException, InterruptedException
        {
            try
            {
                this.runnable.get();
                return new CommandConfirmation();
            }
            catch (ExecutionException e)
            {
                return new CommandConfirmation(e);
            }
        }

        @Override
        public CommandConfirmation getConfirmation(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
        {
            try
            {
                this.runnable.get(timeout, unit);
                return new CommandConfirmation();
            }
            catch (ExecutionException e)
            {
                return new CommandConfirmation(e);
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            return this.runnable.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled()
        {
            return this.runnable.isCancelled();
        }

        @Override
        public boolean isDone()
        {
            return this.runnable.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException
        {
            return this.runnable.get();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
        {
            return this.runnable.get(timeout, unit);
        }
    }

    private static class NullFuture<T> implements QueryFuture<T>
    {
        private final T value;

        NullFuture()
        {
            this(null);
        }

        NullFuture(T returnVal)
        {
            this.value = returnVal;
        }

        @Override
        public CommandConfirmation getConfirmation() throws CancellationException
        {
            return new CommandConfirmation();
        }

        @Override
        public CommandConfirmation getConfirmation(long timeout, TimeUnit unit)
        {
            return new CommandConfirmation();
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
            return this.value;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
        {
            return this.value;
        }
    }

    private interface WrappedRunnable extends Runnable
    {
        default void run()
        {
            try
            {
                runLogic();
            }
            catch (Throwable e)
            {
                logger.error("Could not execute command", e);
            }
        }

        void runLogic();
    }
}
