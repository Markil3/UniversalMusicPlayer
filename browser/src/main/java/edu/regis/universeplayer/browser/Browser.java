/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browser;

import edu.regis.universeplayer.browserCommands.BrowserConstants;
import edu.regis.universeplayer.browserCommands.MessageRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Browser extends MessageRunner
{
    private static final Logger logger = LoggerFactory.getLogger(Browser.class);
    
    private static Browser INSTANCE;
    private static final AtomicBoolean instanceWaiter = new AtomicBoolean();
    
    public static Browser getInstance()
    {
        return INSTANCE;
    }
    
    private final Process process;
    private final ServerSocket server;
    private final Socket socket;
    
    private boolean running = true;
    
    public static Browser createBrowser() throws IOException, InterruptedException
    {
        if (INSTANCE != null)
        {
            return INSTANCE;
        }
        
        ServerSocket server = new ServerSocket(BrowserConstants.PORT, 50, InetAddress.getByName(null));
        logger.debug("Server started.");
        
        int startExit;
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
        INSTANCE = new Browser(socket, server, browserProcess);
        instanceWaiter.set(true);
        notifyAllInstance();
        return INSTANCE;
    }
    
    private Browser(Socket socket, ServerSocket server, Process process) throws IOException
    {
        super("BrowserRunner", socket.getInputStream(), socket.getOutputStream());
        this.socket = socket;
        this.server = server;
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
        return !this.running;
    }
    
    @Override
    protected void onClose()
    {
        logger.debug("Closing socket");
        this.running = false;
        try
        {
            this.socket.close();
        }
        catch (IOException e)
        {
            logger.error("Could not close socket", e);
        }
        finally
        {
            logger.debug("Closing server");
            try
            {
                this.server.close();
            }
            catch (IOException e)
            {
                logger.error("Could not close server", e);
                process.descendants().forEach(ProcessHandle::destroy);
                process.destroy();
            }
        }
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
        File browserDir = new File(System.getProperty("user.dir"), "firefox");
        
        args = " -profile \"" + System.getProperty("user.dir") + "/profile\"";
        logger.info("Running on {} {}", os, arch);
//        System.getProperties().entrySet().stream().forEach(entry -> logger.info("{}: {}", entry.getKey(), entry.getValue()));
        if (os.contains("windows"))
        {
            process = Runtime.getRuntime().exec(new File(browserDir, "firefox.exe").getAbsolutePath() + args);
        }
        else if (os.contains("linux"))
        {
            process = Runtime.getRuntime().exec(new File(browserDir, "firefox").getAbsolutePath() + args);
        }
        if (process == null)
        {
            throw new IOException("Could not find Firefox installation for OS " + os + " " + arch);
        }
        
        return process;
    }
    
    public static void notifyInstance()
    {
        instanceWaiter.notify();
    }
    
    public static void notifyAllInstance()
    {
        synchronized (instanceWaiter)
        {
            instanceWaiter.notifyAll();
        }
    }
    
    public static void waitInstance() throws InterruptedException
    {
        synchronized (instanceWaiter)
        {
            instanceWaiter.wait();
        }
    }
    
    public static void waitInstance(long timeoutMillis) throws InterruptedException
    {
        synchronized (instanceWaiter)
        {
            instanceWaiter.wait(timeoutMillis);
        }
    }
    
    public static void waitInstance(long timeoutMillis, int nanos) throws InterruptedException
    {
        synchronized (instanceWaiter)
        {
            instanceWaiter.wait(timeoutMillis, nanos);
        }
    }
}
