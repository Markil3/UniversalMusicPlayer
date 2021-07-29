/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.addon;

import edu.regis.universeplayer.browserCommands.BrowserConstants;
import edu.regis.universeplayer.browserCommands.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Main
{
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) throws IOException, InterruptedException
    {
        ServerSocket socketSource = null;
        Socket socket = null;
        MessageHandler interfaceLink;
        BrowserLink browserLink;
        Thread interfaceThread, browserThread;
        try
        {
            try
            {
                logger.debug("Connecting to browser");
                browserLink = new BrowserLink("BrowserLink");
                logger.debug("Setting up server");
                socketSource = new ServerSocket(BrowserConstants.PORT);
                logger.debug("Server started");
                socket = socketSource.accept();
                logger.debug("Connection received");
                interfaceLink = new MessageHandler("InterfaceHandler", socket.getInputStream(), socket.getOutputStream());
                /*
                 * Pretty much just forwards any messages to the browser and
                 * returns their value.
                 */
                interfaceLink.addListener((providedValue, previousReturn) -> {
                    logger.debug("Forwarding message to browser: {}", providedValue);
                    Object returnValue = browserLink.sendObject(providedValue).get();
                    logger.debug("Forwarding return to interface: {}", returnValue);
                    return returnValue;
                });
                
                browserThread = new Thread(browserLink);
                interfaceThread = new Thread(interfaceLink);
                
                logger.debug("Starting threads.");
                browserThread.start();
                interfaceThread.start();
                logger.debug("Joining threads.");
                browserThread.join();
                interfaceThread.join();
                logger.debug("Threads completed.");
            }
            catch (IOException e)
            {
                logger.error("Could not initialize socket", e);
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
                finally
                {
                    logger.debug("Shutting down server");
                    try
                    {
                        socketSource.close();
                    }
                    catch (IOException e)
                    {
                        logger.error("Could not close server", e);
                    }
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
