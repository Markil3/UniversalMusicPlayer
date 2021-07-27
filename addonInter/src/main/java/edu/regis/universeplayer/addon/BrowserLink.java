/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.addon;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The browser link serves as a communication between this process and the
 * browser. It automatically converts information as needed as it passes it to
 * and from the browser process.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class BrowserLink implements Runnable
{
    private static Logger logger = LoggerFactory.getLogger(BrowserLink.class);
    public static final Gson gson = new Gson();
    
    private BufferedInputStream browserIn;
    private BufferedOutputStream browserOut;
    
    private final Object readLock = new Object();
    
    /**
     * This queue serves as a cache for objects we need to send.
     */
    private final LinkedList<MessagePacket> sendQueue = new LinkedList<>();
    /**
     * This queue serves as a cache for objects that are waiting for a response.
     */
    private final LinkedList<MessagePacket> sentQueue = new LinkedList<>();
    
    private boolean running;
    
    @Override
    public void run()
    {
        MessagePacket packet;
        this.running = true;
        try
        {
            browserIn = new BufferedInputStream(System.in);
            browserOut = new BufferedOutputStream(System.out);
            
            while (this.running)
            {
                /*
                 * Sends all messages
                 */
                synchronized (this.sendQueue)
                {
                    packet = this.sendQueue.poll();
                }
                if (packet != null)
                {
                    try
                    {
                        logger.debug("Writing message {} {}", packet.message[0], packet.message[1]);
                        this.browserOut.write(packet.message[0]);
                        this.browserOut.write(packet.message[1]);
                        this.browserOut.flush();
                        
                        synchronized (this.sentQueue)
                        {
                            this.sentQueue.add(packet);
                        }
                    }
                    catch (IOException e)
                    {
                        logger.error("Could not send message " + new String(packet.message[1], StandardCharsets.UTF_8), e);
                    }
                }
                
                try
                {
                    packet = null;
                    synchronized (this.sentQueue)
                    {
                        if (this.sentQueue.size() > 0)
                        {
                            packet = this.sentQueue.poll();
                        }
                    }
                    if (packet != null)
                    {
                        if (this.browserIn.available() > 0)
                        {
                            /*
                             * Wait for a response from the browser.
                             */
                            byte[][] returnMessage = this.readMessage();
                            if (returnMessage == null)
                            {
                                logger.info("Browser connection closed.");
                                this.running = false;
                            }
                            else
                            {
                                logger.debug("Reading message {} {}", returnMessage[0], returnMessage[1]);
                                packet.returnMessage = returnMessage;
                                synchronized (this.readLock)
                                {
                                    logger.trace("Received message, notifying futures.");
                                    this.readLock.notifyAll();
                                }
                            }
                        }
                        else
                        {
                            synchronized (this.sentQueue)
                            {
                                this.sentQueue.addFirst(packet);
                            }
                            logger.trace("There are no new messages");
                        }
                    }
                }
                catch (IOException e)
                {
                    logger.error("Could not retrieve message", e);
                }
            }
        }
        finally
        {
            /*
             * Release locks
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
            try
            {
                browserIn.close();
            }
            catch (IOException e1)
            {
                logger.error("Could not close browser input", e1);
            }
            finally
            {
                try
                {
                    browserOut.close();
                }
                catch (IOException e1)
                {
                    logger.error("Could not close browser output", e1);
                }
            }
        }
    }
    
    /**
     * Reads a message from the browser input.
     *
     * @return - A new message packet from the browser, or null if it has been closed.
     * @throws IOException - Should a read error occur.
     */
    private byte[][] readMessage() throws IOException
    {
        ByteBuffer headerReader;
        int rawLength, messageLength;
        byte[] header = new byte[4];
        byte[] message;
        
        rawLength = browserIn.read(header);
        
        /*
         * A value of 0 or -1 indicates that there are no more messages, and
         * that the channel has been closed.
         */
        if (rawLength <= 0)
        {
            logger.info("The browser channel has been closed.");
            return null;
        }
        
        headerReader = ByteBuffer.wrap(header, 0, rawLength);
        /*
         * The browser outputs in whatever the native endian order,
         * which may not be what Java does.
         */
        headerReader.order(ByteOrder.nativeOrder());
        messageLength = headerReader.getInt();
        logger.debug("Receiving message of {} length", messageLength);
        message = new byte[messageLength];
        browserIn.read(message);
        logger.debug("Unpacking message {} (length {})", new String(message, StandardCharsets.UTF_8), messageLength);
        return new byte[][] {header, message};
    }
    
    /**
     * Sends an object to the browser.
     *
     * @param message - The object to send.
     * @return A future for the response from the browser.
     */
    public Future<Object> sendObject(Object message)
    {
        String messageJson;
        byte[] messageData;
        ByteBuffer header;
        byte[][] packet;
        Future<Object> future;
        MessagePacket packetEntry;
        
        logger.debug("Sending message {}", message);
        
        messageJson = gson.toJson(message);
        messageData = messageJson.getBytes(StandardCharsets.UTF_8);
        header = ByteBuffer.allocate(4);
        /*
         * The browser outputs in whatever the native endian order is,
         * which may not be what Java does.
         */
        header.order(ByteOrder.nativeOrder());
        header.putInt(messageData.length);
        
        packetEntry = new MessagePacket(new byte[][] {header.array(), messageData});
        
        future = new Future<>()
        {
            private boolean canceled = false;
            
            @Override
            public boolean cancel(boolean mayInterruptIfRunning)
            {
                synchronized (sendQueue)
                {
                    if (sendQueue.remove(packetEntry))
                    {
                        canceled = true;
                        if (mayInterruptIfRunning)
                        {
                            logger.trace("Canceling future, notifying self futures.");
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
                return packetEntry.returnMessage != null;
            }
            
            @Override
            public Object get() throws InterruptedException, ExecutionException
            {
                logger.trace("Future waiting for message");
                synchronized (readLock)
                {
                    while (!this.canceled && packetEntry.returnMessage == null)
                    {
                        readLock.wait();
                    }
                }
                if (packetEntry.returnMessage == null)
                {
                    throw new InterruptedException();
                }
                logger.trace("Future has received message");
                try
                {
                    return getMessage(packetEntry.returnMessage[1]);
                }
                catch (IOException e)
                {
                    throw new ExecutionException(e);
                }
            }
            
            @Override
            public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
            {
                logger.trace("Future waiting for message");
                synchronized (readLock)
                {
                    while (!this.canceled && packetEntry.returnMessage == null)
                    {
                        readLock.wait(unit.toMillis(timeout));
                    }
                }
                if (packetEntry.returnMessage == null)
                {
                    throw new InterruptedException();
                }
                logger.trace("Future has received message");
                try
                {
                    return getMessage(packetEntry.returnMessage[1]);
                }
                catch (IOException e)
                {
                    throw new ExecutionException(e);
                }
            }
        };
        
        packetEntry.returnValue = future;
        
        synchronized (this.sendQueue)
        {
            sendQueue.add(packetEntry);
        }
        
        return future;
    }
    
    /**
     * Queries for the next message from the browser.
     *
     * @return The received message
     * @throws IOException - If there is an error when reading the next object,
     *                     or if the browser connection is closed.
     */
    private Object getMessage(byte[] messageData) throws IOException
    {
        JsonElement message = gson.fromJson(new String(messageData, StandardCharsets.UTF_8), JsonElement.class);
        return getMessage(message);
    }
    
    /**
     * Translates a JSON element into a Java object
     *
     * @param message - The message to translate.
     * @return A java object, whether it be a primitive wrapper or an actual
     * object.
     * @throws IOException - Thrown if we cannot determine what type of JSON
     *                     element was provided.
     */
    private Object getMessage(JsonElement message) throws IOException
    {
        if (message.isJsonPrimitive())
        {
            return getPrimitiveMessage((JsonPrimitive) message);
        }
        else if (message.isJsonObject())
        {
            return getObjectMessage((JsonObject) message);
        }
        else if (message.isJsonArray())
        {
            return getArrayMessage((JsonArray) message);
        }
        else if (message.isJsonNull())
        {
            return null;
        }
        else
        {
            throw new IOException("Unknown JSON element " + message.getClass());
        }
    }
    
    /**
     * Translates a JSON primitive element into a Java object.
     *
     * @param message - The message to translate.
     * @return A java object, whether it be a {@link Number} object, a
     * {@link Boolean} object, or a {@link String} object.
     * @throws IOException - Thrown if we cannot determine what type of JSON
     *                     element was provided.
     */
    private Object getPrimitiveMessage(JsonPrimitive message) throws IOException
    {
        if (message.isBoolean())
        {
            return message.getAsBoolean();
        }
        else if (message.isNumber())
        {
            return message.getAsNumber();
        }
        else if (message.isString())
        {
            return message.getAsString();
        }
        else
        {
            throw new IOException("Unknown JSON element " + message.getClass());
        }
    }
    
    /**
     * Translates a JSON object element into a Java object.
     *
     * @param message - The message to translate.
     * @return A java object, or a {@link HashMap} containing objects if we
     * cannot parse it.
     */
    private Object getObjectMessage(JsonObject message)
    {
        HashMap<String, Object> type;
        Class<?> clazz = null;
        if (message.has("type"))
        {
            try
            {
                clazz = Class.forName(message.getAsJsonPrimitive("type").getAsString());
            }
            catch (ClassNotFoundException | ClassCastException e)
            {
                logger.error("Illegal class type " + message.get("type"), e);
            }
        }
        
        if (clazz != null)
        {
            return gson.fromJson(message, clazz);
        }
        else
        {
            /*
             * If all else fails, just return a standard hashmap. That should work.
             */
            type = new HashMap<>();
            message.entrySet().forEach(entry -> {
                try
                {
                    type.put(entry.getKey(), getMessage(entry.getValue()));
                }
                catch (IOException e)
                {
                    logger.error("Could not parse message entry " + entry.getKey() + " (" + entry.getValue() + ")", e);
                }
            });
            return type;
        }
    }
    
    /**
     * Translates a JSON array element into a Java array.
     *
     * @param message - The message to translate.
     * @return A java array. It may contain a variety of element types.
     * @throws IOException - Thrown if we cannot determine what type of JSON
     *                     element was provided.
     */
    private Object getArrayMessage(JsonArray message) throws IOException
    {
        Object[] returnValue = new Object[message.size()];
        for (int i = 0, l = returnValue.length; i < l; i++)
        {
            returnValue[i] = getMessage(message.get(i));
        }
        return returnValue;
    }
    
    /**
     * This represents a sent message in storage.
     */
    private class MessagePacket
    {
        public final byte[][] message;
        public byte[][] returnMessage;
        public Future<Object> returnValue;
        
        public MessagePacket(byte[][] message)
        {
            this(message, null);
        }
        
        public MessagePacket(byte[][] message, Future<Object> returnValue)
        {
            this.message = message;
            this.returnValue = returnValue;
        }
    }
}
