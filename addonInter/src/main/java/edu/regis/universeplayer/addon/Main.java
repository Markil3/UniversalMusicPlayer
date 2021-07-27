/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.addon;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class Main
{
    private static Logger logger = LoggerFactory.getLogger(Main.class);
    
    private static final Gson gson = new Gson();
    
    private static BufferedInputStream browserIn;
    private static BufferedOutputStream browserOut;
    
    public static void main(String[] args)
    {
        boolean running = true;
        int rawLength, messageLength;
        byte[] header = new byte[4];
        byte[] message;
        JsonElement messageJson;
        JsonPrimitive jsonPrimative;
        JsonObject jsonObject;
        ByteBuffer headerReader;
        
        System.err.printf("Working from directory %s\n", System.getProperty("user.dir"));
        if (ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN))
        {
            logger.info("Incoming messages will be Big-endian");
        }
        else
        {
            logger.info("Incoming messages will be Little-endian");
        }
        try
        {
            browserIn = new BufferedInputStream(System.in);
            browserOut = new BufferedOutputStream(System.out);
            while (running)
            {
                try
                {
                    rawLength = browserIn.read(header);
                    
                    if (rawLength <= 0)
                    {
                        running = false;
                        break;
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
                    logger.debug("Unpacking message {} (length {})", new String(message, "UTF-8"), messageLength);
                    
                    messageJson = gson.fromJson(new String(message, StandardCharsets.UTF_8), JsonElement.class);
                    logger.debug("Reading JSON element {} (type: {})", messageJson, messageJson.getClass());
                    
                    if (messageJson.isJsonPrimitive())
                    {
                        jsonPrimative = ((JsonPrimitive) messageJson);
                        if (jsonPrimative.isBoolean())
                        {
                            handleMessage(jsonPrimative.getAsBoolean());
                        }
                        else if (jsonPrimative.isNumber())
                        {
                            handleMessage(jsonPrimative.getAsNumber());
                        }
                        else if (jsonPrimative.isString())
                        {
                            handleMessage(jsonPrimative.getAsString());
                        }
                    }
                    else if (messageJson.isJsonObject())
                    {
                        jsonObject = ((JsonObject) messageJson);
                        // TODO - Parse the object into Java objects
                    }
                }
                catch (IOException e)
                {
                    logger.error("Could not create input stream", e);
                    // Forward it to the browser via STDERR
                    e.printStackTrace();
                }
            }
        }
        finally
        {
            try
            {
                browserIn.close();
            }
            catch (IOException e1)
            {
                logger.error("Could not close browser input", e1);
            }
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
    
    /**
     * A callback for when a message is sent from the browser
     *
     * @param message - The message sent.
     */
    private static void handleMessage(Object message)
    {
        if (message instanceof String)
        {
            if (message.equals("ping"))
            {
                sendMessage("pong");
            }
        }
    }
    
    /**
     * A callback for when a message is sent from the browser
     *
     * @param message - The message sent.
     */
    public static void sendMessage(Object message)
    {
        String messageJson;
        byte[] messageData;
        ByteBuffer headerWriter;
        
        logger.debug("Sending message {}", message);
        
        messageJson = gson.toJson(message);
        messageData = messageJson.getBytes(StandardCharsets.UTF_8);
        headerWriter = ByteBuffer.allocate(4);
        /*
         * The browser outputs in whatever the native endian order,
         * which may not be what Java does.
         */
        headerWriter.order(ByteOrder.nativeOrder());
        headerWriter.putInt(messageData.length);
        logger.debug("Writing message {} {}", headerWriter.array(), messageJson);
        try
        {
            browserOut.write(headerWriter.array());
            browserOut.write(messageData);
            browserOut.flush();
        }
        catch (IOException e)
        {
            logger.error("Could not write message", e);
            e.printStackTrace();
        }
    }
}
