/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browser;

import edu.regis.universeplayer.Player;
import edu.regis.universeplayer.browserCommands.*;
import edu.regis.universeplayer.data.Song;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
    private final Socket socket;
    private final Process process;
    
    public static Browser createBrowser() throws IOException, InterruptedException
    {
        /*
         * The maximum number of attempts that will be made to establish a
         * connection.
         */
        final int MAX_ATTEMPTS = 20;
        /*
         * How long the thread will sleep between connection attempts, in
         * milliseconds.
         */
        final long SLEEP_TIME = 500;
        int startExit;
        Socket socket = null;
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
        for (int attempts = 0; socket == null && attempts < MAX_ATTEMPTS; attempts++)
        {
            try
            {
                socket = new Socket(BrowserConstants.IP, BrowserConstants.PORT);
            }
            catch (ConnectException e)
            {
                connErr = e;
                logger.debug("Connection attempt {} failed, trying again", attempts);
                Thread.sleep(SLEEP_TIME);
            }
        }
        if (socket == null)
        {
            throw connErr;
        }
        logger.debug("Browser connection established.");
        browser = new Browser(socket, browserProcess);
        return browser;
    }
    
    private Browser(Socket socket, Process process) throws IOException
    {
        super("BrowserRunner", socket.getInputStream(), socket.getOutputStream());
        this.socket = socket;
        this.process = process;
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
        return null;
    }
    
    @Override
    public QueryFuture<Void> loadSong(InternetSong song)
    {
        try
        {
            return new ForwardedFuture<Void>(this.sendObject(new CommandLoadSong(song.location)));
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
        if (process != null)
        {
            logger.info("Destroying browser processes {}, {}", process, process.descendants().toArray(ProcessHandle[]::new));
            process.descendants().forEach(ProcessHandle::destroy);
            process.destroy();
        }
        else
        {
            logger.info("Process already destroyed.");
        }
        try
        {
            this.socket.close();
        }
        catch (IOException e)
        {
            logger.error("Could not close browser socket", e);
        }
        return null;
    }
    
    @Override
    public QueryFuture<Void> play()
    {
        return null;
    }
    
    @Override
    public QueryFuture<Void> pause()
    {
        return null;
    }
    
    @Override
    public QueryFuture<Void> togglePlayback()
    {
        return null;
    }
    
    @Override
    public QueryFuture<Void> seek(float time)
    {
        return null;
    }
    
    @Override
    public QueryFuture<Boolean> isPaused()
    {
        return null;
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
        private final Future future;
        
        ForwardedFuture(Future future)
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
