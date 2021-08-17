/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browser;

import edu.regis.universeplayer.PlaybackListener;
import edu.regis.universeplayer.PlaybackStatus;
import edu.regis.universeplayer.Player;
import edu.regis.universeplayer.browserCommands.*;
import edu.regis.universeplayer.data.Song;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.*;

/**
 * This serves as a central point for controlling the browser process.
 *
 * @author William Hubbard
 * @since 0.1
 */
public class Browser extends MessageRunner implements Player<InternetSong>
{
    private static final Logger logger = LoggerFactory.getLogger(Browser.class);
    private final Process process;
    private final Socket socket;
    
    private final LinkedList<PlaybackListener> listeners = new LinkedList<>();
    
    private InternetSong currentSong;
    
    public static Browser createBrowser() throws IOException, InterruptedException
    {
        ServerSocket server = new ServerSocket(BrowserConstants.PORT, 50, InetAddress.getByName(null));
        logger.debug("Server started.");
        
        int startExit;
        Browser browser;
        Process browserProcess = launchBrowser();
        /*
         * Wait for the browser to fully start.
         */
        startExit = browserProcess.waitFor();
        if (startExit != 0)
        {
            logger.error("Error in browser launch (exit code {})", startExit);
            try (Scanner scanner = new Scanner(browserProcess.getErrorStream()))
            {
                while (scanner.hasNextLine())
                {
                    logger.error(scanner.nextLine());
                }
            }
            throw new IOException("Error in browser launch (exit code " + startExit + ")");
        }
        logger.debug("Browser started.");
        
        ConnectException connErr = null;
        logger.debug("Attempting connection");
        Socket socket = server.accept();
        if (!socket.isBound())
        {
            logger.error("Socket not bound");
        }
        else if (!socket.isConnected())
        {
            logger.error("Socket not connected");
        }
        else if (socket.isClosed())
        {
            logger.error("Socket prematurely closed");
        }
        else if (socket.isInputShutdown())
        {
            logger.error("Socket input prematurely closed.");
        }
        else if (socket.isOutputShutdown())
        {
            logger.error("Socket input prematurely closed.");
        }
        else
        {
            logger.debug("Connection established.");
        }
        browser = new Browser(socket, browserProcess);
        return browser;
    }
    
    private Browser(Socket socket, Process process) throws IOException
    {
        super("BrowserRunner", socket.getInputStream(), socket.getOutputStream());
        this.socket = socket;
        this.process = process;
    }
    
    @Override
    protected boolean onRun()
    {
        if (!socket.isConnected() || socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown())
        {
            logger.debug("Socket closed, shutting down");
            return true;
        }
        return false;
    }
    
    /**
     * Utility method for launching a browser instance
     *
     * @throws IOException - Thrown if there is a problem launching the browser.
     */
    private static Process launchBrowser() throws IOException
    {
        Process process = null;
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String args;
        File browserDir = new File(System.getProperty("user.dir"), "browser");
        
        if (!browserDir.exists())
        {
            browserDir = new File(System.getProperty("user.dir"), "../browser");
        }
        args = " -profile \"" + browserDir.getAbsolutePath() + "/profile\"";
        logger.info("Running on {} {}", os, arch);
//        System.getProperties().entrySet().stream().forEach(entry -> logger.info("{}: {}", entry.getKey(), entry.getValue()));
        if (os.contains("windows"))
        {
            if (arch.contains("64"))
            {
                logger.debug("Starting Windows x86_64 browser");
                process = Runtime.getRuntime().exec(new File(browserDir, "windows64/firefox.exe").getAbsolutePath() + args);
            }
            else
            {
                logger.debug("Starting Windows x86 browser");
                process = Runtime.getRuntime().exec(new File(browserDir, "windows32/firefox.exe").getAbsolutePath() + args);
            }
        }
        else if (os.contains("linux"))
        {
            if (arch.contains("64"))
            {
                logger.debug("Starting Linux x86_64 browser");
                process = Runtime.getRuntime().exec(new File(browserDir, "linux64/firefox").getAbsolutePath() + args);
            }
            else
            {
                logger.debug("Starting Linux 86 browser");
                process = Runtime.getRuntime().exec(new File(browserDir, "linux32/firefox").getAbsolutePath() + args);
            }
        }
        if (process == null)
        {
            throw new IOException("Could not find Firefox installation for OS " + os + " " + arch);
        }
        
        return process;
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
            return new ForwardedFuture(this.sendObject(new CommandLoadSong(song.location)));
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
            QueryFuture<Void> future = new ForwardedFuture(this.sendObject(new CommandQuit()));
            return future;
        }
        catch (IOException e)
        {
            logger.error("Could not deliver quit command", e);
            logger.error("Destroying browser processes {}, {}", process, process.descendants().toArray(ProcessHandle[]::new));
            try
            {
                socket.close();
            }
            catch (IOException ex)
            {
                logger.error("Could not close socket", ex);
            }
            finally
            {
                process.descendants().forEach(ProcessHandle::destroy);
                process.destroy();
            }
        }
        return null;
    }

    /**
     * Adds a listener for playback status updates.
     *
     * @param listener - The listener to add.
     */
    @Override
    public void addPlaybackListener(PlaybackListener listener)
    {

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
        return false;
    }

    /**
     * Removes a listener for playback status updates.
     *
     * @param listener - The listener to remove.
     */
    @Override
    public void removePlaybackListener(PlaybackListener listener)
    {

    }

    @Override
    public QueryFuture<Void> play()
    {
        try
        {
            return new ForwardedFuture(this.sendObject(new CommandSetPlayback(CommandSetPlayback.Playback.PLAY)));
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
            return new ForwardedFuture(this.sendObject(new CommandSetPlayback(CommandSetPlayback.Playback.PAUSE)));
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
            return new ForwardedFuture(this.sendObject(new CommandSetPlayback(CommandSetPlayback.Playback.PAUSE)));
        }
        catch (IOException e)
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
        return null;
    }

    @Override
    public QueryFuture<Void> seek(float time)
    {
        try
        {
            return new ForwardedFuture(this.sendObject(new CommandSeek(time)));
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
            Future future = this.sendObject(new QueryStatus());
            return new QueryFuture<>() {
                
                private CommandReturn<Double> getVal() throws ExecutionException, InterruptedException
                {
                    return ((CommandReturn<Double>) future.get());
                }
    
                private CommandReturn<Double> getVal(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException
                {
                    return ((CommandReturn<Double>) future.get(timeout, unit));
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
                    Double value = getVal().getReturnValue();
                    return PlaybackStatus.values()[value.intValue()];
                }
    
                @Override
                public PlaybackStatus get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
                {
                    Double value = getVal(timeout, unit).getReturnValue();
                    return PlaybackStatus.values()[value.intValue()];
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
