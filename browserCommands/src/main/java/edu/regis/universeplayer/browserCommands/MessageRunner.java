/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class serves as a general inter-process object messaging system.
 */
public abstract class MessageRunner implements Runnable, MessageSerializer
{
    private final Logger logger;
    
    private final String name;
    private final InputStream input;
    private final OutputStream output;
    private final Object readLock = new Object();
    
    /**
     * This queue serves as a cache for objects we need to send.
     */
    private final LinkedList<MessagePacket> sendQueue = new LinkedList<>();
    /**
     * This queue serves as a cache for objects that are waiting for a response.
     */
    private final HashMap<Integer, MessagePacket> sentQueue = new HashMap<>();
    private int messagesSent = 0;
    
    /**
     * Creates a message runner.
     *
     * @param name - The name of the runner. This is used in logging.
     * @param input  - The input from our external source.
     * @param output - The output to the external source.
     */
    public MessageRunner(String name, InputStream input, OutputStream output)
    {
        this.name = name;
        this.logger = LoggerFactory.getLogger(name);
        this.input = input;
        this.output = output;
    }
    
    @Override
    public Logger getLogger()
    {
        return logger;
    }
    
    @Override
    public void run()
    {
        BufferedInputStream browserIn = null;
        BufferedOutputStream browserOut = null;
        MessagePacket packet;
    
        boolean running = true;
        
        byte[][] returnMessage;
        ByteBuffer numBuffer = ByteBuffer.allocate(4);
        
        try
        {
            browserIn = new BufferedInputStream(this.input);
            browserOut = new BufferedOutputStream(this.output);
            
            while (running)
            {
                /*
                 * Sends a messages
                 */
                synchronized (this.sendQueue)
                {
                    packet = this.sendQueue.poll();
                }
                if (packet != null)
                {
                    try
                    {
                        packet.returnValue.index = this.messagesSent;
                        writeMessage(browserOut, this.messagesSent, packet.message);
                        
                        synchronized (this.sentQueue)
                        {
                            this.sentQueue.put(this.messagesSent, packet);
                            this.messagesSent++;
                        }
                    }
                    catch (IOException e)
                    {
                        logger.error("Could not send message " + this.messagesSent + " " + new String(packet.message, StandardCharsets.UTF_8), e);
                    }
                }
                
                /*
                 * Reads any available messages
                 */
                try
                {
                    if (!this.sentQueue.isEmpty())
                    {
                        if (browserIn.available() > 0)
                        {
                            /*
                             * Wait for a response from the browser.
                             */
                            returnMessage = this.readMessage(browserIn);
                            if (returnMessage == null)
                            {
                                logger.info("Connection closed.");
                                running = false;
                            }
                            else
                            {
                                numBuffer.clear();
                                numBuffer.put(returnMessage[0]);
                                numBuffer.clear();
                                packet = this.sentQueue.get(numBuffer.getInt());
                                if (packet == null)
                                {
                                    numBuffer.clear();
                                    logger.warn("Received message {} for nonexistant packet", numBuffer.getInt());
                                }
                                else
                                {
                                    packet.returnMessage = returnMessage[1];
                                    numBuffer.clear();
                                    logger.debug("Reading message {} {}", numBuffer.getInt(), packet.returnMessage);
                                    synchronized (this.readLock)
                                    {
                                        logger.trace("Received message {}, notifying futures.", packet.returnValue.index);
                                        this.readLock.notifyAll();
                                    }
                                }
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    logger.error("Could not retrieve message", e);
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
            synchronized (this.sendQueue)
            {
                while (this.sendQueue.size() > 0)
                {
                    packet = this.sendQueue.poll();
                    packet.returnMessage = null;
                }
            }
            synchronized (this.readLock)
            {
                this.readLock.notifyAll();
            }
            /*
             * Close the streams.
             */
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
            }
        }
    }
    
    /**
     * Sends an object to the remote.
     *
     * @param message - The message to send.
     * @return A future for whatever the remote may return.
     * @throws IOException Should an error occur in serializing the object.
     */
    public Future<Object> sendObject(Object message) throws IOException
    {
        byte[] messageData = this.serializeObject(message);
        MessagePacket packetEntry = new MessagePacket(messageData);
        MessageFuture future = new MessageFuture(packetEntry);
        
        synchronized (this.sendQueue)
        {
            sendQueue.add(packetEntry);
        }
        
        return future;
    }
    
    /**
     * This represents a sent message in storage.
     */
    public static class MessagePacket
    {
        public final byte[] message;
        public byte[] returnMessage;
        public MessageFuture returnValue;
        
        public MessagePacket(byte[] message)
        {
            this(message, null);
        }
        
        public MessagePacket(byte[] message, MessageFuture returnValue)
        {
            this.message = message;
            this.returnValue = returnValue;
        }
    }
    
    /**
     * This future waits for a return message from the runner.
     */
    public class MessageFuture implements Future<Object>
    {
        private final MessagePacket packetEntry;
        private int index;
        private boolean canceled = false;
        
        private MessageFuture(MessagePacket packet)
        {
            this.packetEntry = packet;
            packet.returnValue = this;
        }
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            synchronized (sendQueue)
            {
                if (sendQueue.remove(this.packetEntry))
                {
                    this.canceled = true;
                    if (mayInterruptIfRunning)
                    {
                        logger.trace("Canceling future, notifying self futures.");
                        synchronized (sentQueue)
                        {
                            sentQueue.remove(this.index);
                        }
                        synchronized (readLock)
                        {
                            readLock.notifyAll();
                        }
                    }
                    else
                    {
                        logger.trace("Canceling future.");
                    }
                    return true;
                }
                else
                {
                    return false;
                }
            }
        }
        
        @Override
        public boolean isCancelled()
        {
            return this.canceled;
        }
        
        @Override
        public boolean isDone()
        {
            return this.packetEntry.returnMessage != null;
        }
        
        @Override
        public Object get() throws InterruptedException, ExecutionException
        {
            if (this.isDone())
            {
                try
                {
                    return deserializeObject(this.packetEntry.returnMessage);
                }
                catch (IOException | ClassNotFoundException e)
                {
                    throw new ExecutionException("Error parsing object", e);
                }
            }
            synchronized (readLock)
            {
                while (!this.canceled && packetEntry.returnMessage == null)
                {
                    logger.trace("Future {} waiting for message", this.index);
                    readLock.wait();
                }
            }
            if (packetEntry.returnMessage == null)
            {
                throw new InterruptedException();
            }
            logger.trace("Future {} has received message", this.index);
            try
            {
                return deserializeObject(this.packetEntry.returnMessage);
            }
            catch (IOException | ClassNotFoundException e)
            {
                throw new ExecutionException("Error parsing object", e);
            }
        }
        
        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
        {
            if (this.isDone())
            {
                try
                {
                    return deserializeObject(this.packetEntry.returnMessage);
                }
                catch (IOException | ClassNotFoundException e)
                {
                    throw new ExecutionException("Error parsing object", e);
                }
            }
            synchronized (readLock)
            {
                while (!this.canceled && packetEntry.returnMessage == null)
                {
                    logger.trace("Future {} waiting for message", this.index);
                    readLock.wait(unit.toMillis(timeout));
                }
            }
            if (packetEntry.returnMessage == null)
            {
                throw new InterruptedException();
            }
            logger.trace("Future {} has received message", this.index);
            try
            {
                return deserializeObject(this.packetEntry.returnMessage);
            }
            catch (IOException | ClassNotFoundException e)
            {
                throw new ExecutionException("Error parsing object", e);
            }
        }
    }
}
