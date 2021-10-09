/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.DefaultLoggerContextAccessor;
import org.apache.logging.log4j.core.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.regis.universeplayer.ConfigManager;
import edu.regis.universeplayer.Log;
import edu.regis.universeplayer.browserCommands.BrowserConstants;
import edu.regis.universeplayer.browserCommands.MessageRunner;
import edu.regis.universeplayer.browserCommands.UpdateListener;

public class Browser extends MessageRunner
{
    private static final Logger logger = LoggerFactory.getLogger(Browser.class);
    private static final Logger browserLogger =
            LoggerFactory.getLogger("browser");

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

        ServerSocket server = new ServerSocket(BrowserConstants.PORT, 50, InetAddress
                .getByName(null));
        logger.debug("Server started.");

        int startExit;
        Process browserProcess = launchBrowser();
        /*
         * Wait for the browser to fully start.
         */
        if (!browserProcess.isAlive())
        {
            startExit = browserProcess.exitValue();
            if (startExit != 0)
            {
                logger.error("Error in browser launch (exit code {})", startExit);
                try (Scanner scanner = new Scanner(browserProcess
                        .getErrorStream()))
                {
                    while (scanner.hasNextLine())
                    {
                        logger.error(scanner.nextLine());
                    }
                }
                notifyAllInstance();
                throw new IOException("Error in browser launch (exit code " + startExit + ")");
            }
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
        super("BrowserRunner", socket.getInputStream(), socket
                .getOutputStream());
        this.socket = socket;
        this.server = server;
        this.process = process;

        /*
         * Sends browser logs to the main log.
         */
        this.addUpdateListener((object, runner) ->
        {
            Log log;
            LogEvent logEvent;
            if (object instanceof Log)
            {
                log = (Log) object;
                String methodName;
                Class<?>[] types = null;
                Object[] params = null;
                if (log.message.length == 1)
                {
                    types = new Class<?>[]{String.class};
                    params = new Object[]{log.message[0]};
                }
                else if (log.message.length == 2)
                {
                    types = new Class<?>[]{String.class, Object.class};
                    params = new Object[]{log.message[0], log.message[1]};
                }
                else if (log.message.length == 3)
                {
                    types = new Class<?>[]{String.class, Object.class,
                            Object[].class};
                    params = new Object[]{log.message[0], log.message[1],
                            log.message[1]};
                }
                else if (log.message.length > 4)
                {
                    types = new Class<?>[]{String.class, Object[].class};
                    params = new Object[]{log.message[0],
                            Arrays.copyOfRange(log.message, 1,
                                    log.message.length)};
                }
                methodName = log.level.toLowerCase();
                if (types != null)
                {
                    Method method = null;
                    try
                    {
                        method = Logger.class.getMethod(methodName, types);
                        method.invoke(LoggerFactory.getLogger(log.logger), params);
                    }
                    catch (NoSuchMethodException e)
                    {
                        /*
                         * This can happen if a zero-argument message was
                         * received. This is expected.
                         */
                    }
                    catch (IllegalAccessException | InvocationTargetException e)
                    {
                        logger.error("Could not process logger message {}", log,
                                e);
                    }
                }
            }
            else if (object instanceof LogEvent)
            {
                DefaultLoggerContextAccessor.INSTANCE.getLoggerContext().getRootLogger().get().log((LogEvent) object);
            }
        });
    }

    @Override
    protected boolean onRun()
    {
        if (!socket.isConnected() || socket.isClosed() || socket
                .isInputShutdown() || socket.isOutputShutdown())
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
     *
     */
    public void stop()
    {
        this.running = false;
    }

    /**
     * Utility method for launching a browser instance
     *
     * @throws IOException - Thrown if there is a problem launching the
     *                     browser.
     */
    private static Process launchBrowser() throws IOException, InterruptedException
    {
        Process process = null;
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String args;
        File browserDir = ConfigManager.getFirefoxDir();

//        args = " -no-remote -profile \"" + profileDir.getAbsolutePath() + "\"";
        args = " -no-remote -P Universal";
        logger.info("Running on {} {}", os, arch);
//        System.getProperties().entrySet().stream().forEach(entry -> logger.info("{}: {}", entry.getKey(), entry.getValue()));
        File firefox = null;
        if (os.contains("windows"))
        {
            firefox = new File(browserDir, "firefox.exe");
        }
        else if (os.contains("linux"))
        {
            firefox = new File(browserDir, "firefox");
        }
        if (firefox != null)
        {
            firefox = firefox.getAbsoluteFile();
            Runtime.getRuntime().exec(firefox + " -CreateProfile Universal").waitFor();
            process = Runtime.getRuntime().exec(firefox
                    .getAbsolutePath() + args);
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
