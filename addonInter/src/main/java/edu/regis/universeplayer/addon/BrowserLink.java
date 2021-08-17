/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.addon;

import com.google.gson.*;
import edu.regis.universeplayer.browserCommands.MessageRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * The browser link serves as a communication between this process and the
 * browser. It automatically converts information as needed as it passes it to
 * and from the browser process.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class BrowserLink extends MessageRunner
{
    private static final Logger logger = LoggerFactory.getLogger(BrowserLink.class);
    public static final Gson gson;
    
    static
    {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(StackTraceElement.class, new StackTraceElementSerializer());
        builder.registerTypeAdapter(Throwable.class, new ThrowableSerializer());
        gson = builder.create();
    }
    
    /**
     * Creates a message runner.
     */
    public BrowserLink(String name)
    {
        super(name, System.in, System.out);
    }
    
    @Override
    public byte[] serializeObject(Object message)
    {
        JsonElement val = gson.toJsonTree(message);
        if (val.isJsonObject())
        {
            ((JsonObject) val).addProperty("type", message.getClass().getName());
        }
        return val.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    @Override
    public Object deserializeObject(byte[] message) throws IOException
    {
        JsonElement val = gson.fromJson(new String(message, StandardCharsets.UTF_8), JsonElement.class);
        return getMessage(val);
    }
    
    @Override
    public void writeMessage(OutputStream out, int messageNum, byte[] message) throws IOException
    {
        String messageString;
        JsonObject sendOb = new JsonObject();
        sendOb.add("messageNum", new JsonPrimitive(messageNum));
        sendOb.add("message", gson.fromJson(new String(message, StandardCharsets.UTF_8), JsonElement.class));
        messageString = gson.toJson(sendOb);
        message = messageString.getBytes(StandardCharsets.UTF_8);
        
        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        lengthBuffer.order(ByteOrder.nativeOrder());
        getLogger().debug("Writing message {} {}", messageNum, messageString);
        /*
         * Writes the message length
         */
        lengthBuffer.clear();
        lengthBuffer.putInt(message.length);
        out.write(lengthBuffer.array());
        /*
         * Writes the message
         */
        out.write(message);
        out.flush();
    }
    
    @Override
    public byte[][] readMessage(InputStream in) throws IOException
    {
        int messageLength;
        int messageNum;
        int readLength;
        byte[] message;
        JsonObject messageJson;
        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        lengthBuffer.order(ByteOrder.nativeOrder());
        lengthBuffer.clear();
        readLength = in.read(lengthBuffer.array());
        if (readLength == 0)
        {
            getLogger().debug("Input stream closed, no more messages.");
            return null;
        }
        messageLength = lengthBuffer.getInt();
        message = new byte[messageLength];
        
        readLength = in.read(message);
        if (readLength < message.length)
        {
            getLogger().warn("Message shorter than reported (expected {} bytes, got {} bytes)", message.length, readLength);
        }
        messageJson = gson.fromJson(new String(message, StandardCharsets.UTF_8), JsonObject.class);
        messageNum = messageJson.get("messageNum").getAsInt();
        message = gson.toJson(messageJson.get("message")).getBytes(StandardCharsets.UTF_8);
        
        lengthBuffer.order(ByteOrder.BIG_ENDIAN);
        /*
         * Wipe out the buffer
         */
        lengthBuffer.clear();
        lengthBuffer.put(new byte[4]);
        lengthBuffer.clear();
        lengthBuffer.putInt(messageNum);
        getLogger().debug("Reading message {}", messageNum);
        return new byte[][] {lengthBuffer.array(), message};
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
                message.remove("type");
            }
            catch (ClassNotFoundException | ClassCastException e)
            {
                getLogger().error("Illegal class type " + message.get("type"), e);
            }
        }
        
        if (clazz != null)
        {
            try
            {
                return gson.fromJson(message, clazz);
            }
            catch (Exception e)
            {
                logger.error("Could not parse " + message, e);
                throw e;
            }
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
                    getLogger().error("Could not parse message entry " + entry.getKey() + " (" + entry.getValue() + ")", e);
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
}
