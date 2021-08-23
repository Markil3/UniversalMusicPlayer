/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Dispatches messages sent from a corresponding {@link MessageRunner}.
 */
public class MessageHandler implements Runnable, MessageSerializer
{
    private final Logger logger;
    
    public final String name;
    
    private final InputStream input;
    private final OutputStream output;
    
    private final ExecutorService executor;
    private final LinkedList<MessageListener> listeners = new LinkedList<>();
    protected final HashMap<Integer, Future<Object>> messageResponses = new HashMap<>();
    protected final Queue<Object> updates = new LinkedList<>();
    
    /**
     * Creates a message handler.
     *
     * @param name   - The name of the handler. This is used in logging.
     * @param input  - The input from our external source.
     * @param output - The output to the external source.
     */
    public MessageHandler(String name, InputStream input, OutputStream output)
    {
        this.name = name;
        this.logger = LoggerFactory.getLogger(name);
        this.input = input;
        this.output = output;
        this.executor = Executors.newCachedThreadPool();
    }
    
    @Override
    public Logger getLogger()
    {
        return logger;
    }
    
    /**
     * Called at the beginning of every loop to do extra processing and check to see if we can still run.
     *
     * @return True if we should close the runner, false otherwise.
     */
    protected boolean onRun()
    {
        return false;
    }
    
    @Override
    public void run()
    {
        BufferedInputStream browserIn = null;
        BufferedOutputStream browserOut = null;
        
        byte[][] message;
        byte[] messageByte;
        Object messageOb;
        Future<Object> messageResponse;
        ByteBuffer numBuffer = ByteBuffer.allocate(4);
        HashSet<Integer> toRemove = new HashSet<>();
        boolean running = true;
        
        try
        {
            browserIn = new BufferedInputStream(this.input);
            browserOut = new BufferedOutputStream(this.output);
            
            while (!this.onRun() && running)
            {
                try
                {
                    if (browserIn.available() > 0)
                    {
                        message = this.readMessage(browserIn);
                        if (message == null)
                        {
                            logger.info("Connection closed.");
                            break;
                        }
                        else
                        {
                            numBuffer.clear();
                            numBuffer.put(message[0]);
                            messageOb = this.deserializeObject(message[1]);
                            messageResponse = this.triggerListeners(messageOb);
                            synchronized (this.messageResponses)
                            {
                                numBuffer.clear();
                                this.messageResponses.put(numBuffer.getInt(), messageResponse);
                            }
                        }
                    }
                }
                catch (IOException | ClassNotFoundException e)
                {
                    logger.error("Could not retrieve message", e);
                    running = false;
                }
                
                /*
                 * Returns any finished responses.
                 */
                synchronized (this.messageResponses)
                {
                    if (this.messageResponses.size() > 0)
                    {
                        for (Map.Entry<Integer, Future<Object>> responses : this.messageResponses.entrySet())
                        {
                            try
                            {
                                if (responses.getValue().isDone())
                                {
                                    toRemove.add(responses.getKey());
                                    messageByte = serializeObject(responses.getValue().get());
                                    writeMessage(browserOut, responses.getKey(), messageByte);
                                }
                            }
                            catch (IOException e)
                            {
                                logger.error("Could not send response message for " + responses.getKey(), e);
                                running = false;
                            }
                        }
                        toRemove.forEach(this.messageResponses::remove);
                        toRemove.clear();
                    }
                }
                
                /*
                 * Send in any updates
                 */
                synchronized (this.updates)
                {
                    if (this.updates.size() > 0)
                    {
                        Object update;
                        while ((update = this.updates.poll()) != null)
                        {
                            try
                            {
                                messageByte = serializeObject(update);
                                writeMessage(browserOut, -1, messageByte);
                            }
                            catch (IOException e)
                            {
                                logger.error("Could not send response message for" + update, e);
                                running = false;
                            }
                        }
                    }
                }
            }
        }
        catch (Throwable e)
        {
            logger.error(this.getClass().getName() + " Error", e);
        }
        finally
        {
            /*
             * Release locks for any messages still waiting.
             */
            synchronized (this.messageResponses)
            {
                logger.debug("Releasing {} dangling messages", this.messageResponses.size());
                this.messageResponses.values().forEach(future -> future.cancel(false));
            }
            /*
             * Close the streams.
             */
            logger.debug("Closing streams");
            try
            {
                if (browserIn != null)
                {
                    browserIn.close();
                }
            }
            catch (IOException e1)
            {
                logger.error("Could not close browser input", e1);
            }
            finally
            {
                try
                {
                    if (browserOut != null)
                    {
                        browserOut.close();
                    }
                }
                catch (IOException e1)
                {
                    logger.error("Could not close browser output", e1);
                }
                finally
                {
                    this.onClose();
                }
            }
        }
    }
    
    /**
     * Callback for when closing the application.
     */
    protected void onClose()
    {
    }
    
    /**
     * Triggers the message listeners.
     *
     * @param message - The message to send.
     * @return The value returned by the listeners.
     */
    protected Future<Object> triggerListeners(Object message)
    {
        MessageListener[] listeners;
        Future<Object> returnVal;
        /*
         * Copies the list of listeners, so we have an unchanging array without
         * holding onto the list for the whole time of execution.
         */
        synchronized (this.listeners)
        {
            listeners = this.listeners.toArray(MessageListener[]::new);
        }
        returnVal = this.executor.submit(() -> {
            Object returnValue = null;
            for (MessageListener listener : listeners)
            {
                returnValue = listener.onMessage(message, returnValue);
            }
            return returnValue;
        });
        return returnVal;
    }
    
    /**
     * Adds a message listener
     *
     * @param listener - A callback for when a message arrives. A value needs to
     *                 be returned from one of the listeners.
     */
    public void addListener(MessageListener listener)
    {
        synchronized (this.listeners)
        {
            this.listeners.add(listener);
        }
    }
    
    /**
     * Sends an object through the handler as an update not associated with any message.
     *
     * @param object - The object to send.
     */
    public void sendUpdate(Object object)
    {
        synchronized (this.updates)
        {
            this.updates.add(object);
        }
    }
    
    /**
     * Checks to see if a listener has been added to the list.
     *
     * @param listener - A callback for when a message arrives.
     * @return True if the provided listener is part of the list, false otherwise.
     */
    public boolean hasListener(MessageListener listener)
    {
        synchronized (this.listeners)
        {
            return this.listeners.contains(listener);
        }
    }
    
    /**
     * Removes a message listener from the list.
     *
     * @param listener - A callback for when a message arrives.
     */
    public void removeListener(MessageListener listener)
    {
        synchronized (this.listeners)
        {
            this.listeners.remove(listener);
        }
    }
    
    /**
     * A message listener is called when a message is sent from the remote.
     *
     * @see #addListener(MessageListener)
     * @see #hasListener(MessageListener)
     * @see #removeListener(MessageListener)
     */
    public interface MessageListener
    {
        /**
         * Called when the remote sends a message and wants a response.
         *
         * @param providedValue  - The message sent from the remote.
         * @param previousReturn The value that was returned by the previous
         *                       listener called. A value must be returned to
         *                       the remote. The listener either has the option
         *                       of returning this value, or returning a new
         *                       value. If null, then this is either the first
         *                       listener called or none of the other listeners
         *                       have generated return values.
         * @return The value to be returned to the remote.
         */
        Object onMessage(Object providedValue, Object previousReturn) throws IOException, ExecutionException, InterruptedException;
    }
}
