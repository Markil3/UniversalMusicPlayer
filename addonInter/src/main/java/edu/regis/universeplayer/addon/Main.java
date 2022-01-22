/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.addon;

import edu.regis.universeplayer.QueueAppender;
import edu.regis.universeplayer.browserCommands.BrowserConstants;
import edu.regis.universeplayer.browserCommands.MessageHandler;
import org.apache.logging.log4j.core.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This class is responsible for setting up the intermediary program. The
 * program is launched by the browser addon when the browser (not a tab, the
 * browser) has launched and the addon has loaded. It launches two forwarding
 * streams: one between this application and the browser that translates
 * between the JSON format the browser uses and the serialized Java objects
 * that this program uses (working over console I/O), and a second link to
 * communicate between the interface and the browser link over the localhost
 * (as defined by the socket server the interface sets up).
 */
public class Main
{
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException
    {
        MessageHandler interfaceLink;
        BrowserLink browserLink;
        Thread interfaceThread, browserThread;
        Socket socket = null;
        try
        {
            /*
             * Sets up a two-way data stream to the browser through the
             * process I/O streams. This converts between JSON data used by
             * the browser to serializable Java objects used by the interface.
             */
            logger.debug("Connecting to browser");
            browserLink = new BrowserLink("BrowserLink");
            try
            {
                logger.debug("Setting up connection");
                socket = new Socket(BrowserConstants.IP, BrowserConstants.PORT);
                logger.debug("Connection established");

                /*
                 * Sets up the data stream between this application and the
                 * interface.
                 */
                Socket finalSocket = socket;
                interfaceLink = new MessageHandler("InterfaceHandler", finalSocket.getInputStream(), finalSocket.getOutputStream())
                {
                    private long lastPing = 0;

                    @Override
                    protected boolean onRun()
                    {
                        /*
                         * How many milliseconds must pass between browser
                         * pings before this stream shuts down.
                         */
                        final long PING_RATE = 5000;
                        if (!finalSocket.isConnected() || finalSocket.isClosed() || finalSocket.isInputShutdown() || finalSocket.isOutputShutdown())
                        {
                            logger.debug("Interface socket closed, shutting down");
                            return true;
                        }
                        /*
                         * Make sure that it is active, either with logs from
                         * the browser or just with ping messages sent from
                         * here.
                         */
                        if (QueueAppender.hasLogs())
                        {
                            lastPing = System.currentTimeMillis();
                            for (LogEvent event : QueueAppender.retrieveLogEvents())
                            {
                                this.sendUpdate(event);
                            }
                        }
                        else if (System.currentTimeMillis() - lastPing >= PING_RATE)
                        {
                            lastPing = System.currentTimeMillis();
                            this.sendUpdate("ping");
                        }
                        return false;
                    }

                    @Override
                    protected void onClose()
                    {
                        try
                        {
                            logger.debug("Closing interface socket.");
                            finalSocket.close();
                            browserLink.sendObject("quit");
                        }
                        catch (IOException e)
                        {
                            logger.error("Could not close socket", e);
                        }
                    }
                };
                logger.debug("Connection received");
                /*
                 * Pretty much just forwards any messages to the browser and
                 * returns their value. This is primarily a one-way
                 * relationship, where the interface sends commands, and the
                 * browser returns updates and responses.
                 */
                browserLink.addUpdateListener((update, link) ->
                {
                    logger.debug("Sending update {}", update);
                    interfaceLink.sendUpdate(update);
                });
                interfaceLink.addListener((providedValue, previousReturn) ->
                {
                    logger.debug("Forwarding message to browser: {}", providedValue);
                    Object returnValue = browserLink.sendObject(providedValue).get();
                    logger.debug("Forwarding return to interface: {}", returnValue);
                    return returnValue;
                });

                /*
                 * Sets up both streams on their own threads.
                 */
                browserThread = new Thread(browserLink);
                interfaceThread = new Thread(interfaceLink);

                logger.debug("Starting threads.");
                browserThread.start();
                interfaceThread.start();
                logger.debug("Joining threads.");
                /*
                 * Waits for both threads to shut down.
                 */
                try
                {
                    browserThread.join();
                    interfaceThread.join();
                }
                catch (InterruptedException e)
                {
                    logger.error("Could not wait for threads", e);
                }
                logger.debug("Threads completed.");
            }
            catch (IOException e)
            {
                logger.error("Could not initialize socket", e);
                try
                {
                    browserLink.sendObject("quit");
                }
                catch (IOException ex)
                {
                    logger.error("Could not quit socket", ex);
                }
                throw e;
            }
        }
        catch (Throwable e)
        {
            logger.error("ERROR", e);
            throw e;
        }
        finally
        {
            logger.debug("Shutting down interface link");
            if (socket != null)
            {
                try
                {
                    socket.close();
                }
                catch (IOException e)
                {
                    logger.error("Could not close socket", e);
                }
            }
        }
    }

    private static void testMain() throws ExecutionException, InterruptedException
    {
        LinkedList<Future<Object>> requests = new LinkedList<>();
        BrowserLink link = new BrowserLink("BrowserLink");
        Thread linkThread = new Thread(link);
        linkThread.start();
        logger.info("Sending message");
        logger.error("This is a test of the emergency logging system", new RuntimeException("This is an emergency test"));
        for (int i = 0, l = 20; i < l; i++)
        {
            try
            {
                Future<Object> future = link.sendObject("ping");
                requests.add(future);
            }
            catch (IOException e)
            {
                logger.error("Could not send object");
            }
        }
        while (requests.size() > 0)
        {
            logger.info("Message received: {}", requests.poll().get());
        }
    }
}
