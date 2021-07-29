/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

import org.slf4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;

public interface MessageSerializer
{
    Logger getLogger();
    
    /**
     * Converts an object into a form that can be sent.
     *
     * @param message - The message to send.
     * @return The byte array representation of that object.
     * @throws IOException Should a serialization error occur.
     */
    default byte[] serializeObject(Object message) throws IOException
    {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream())
        {
            try (ObjectOutputStream stream = new ObjectOutputStream(byteStream))
            {
                stream.writeObject(message);
            }
            return byteStream.toByteArray();
        }
    }
    
    /**
     * Converts a byte stream into an object.
     *
     * @param message - The message received.
     * @return An object.
     * @throws IOException            If there is an error in parsing the message.
     * @throws ClassNotFoundException If the object is not recognized
     */
    default Object deserializeObject(byte[] message) throws IOException, ClassNotFoundException
    {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(message))
        {
            try (ObjectInputStream stream = new ObjectInputStream(byteStream))
            {
                return stream.readObject();
            }
        }
    }
    
    /**
     * Writes a message to the output stream.
     *
     * @param out        - The output stream to write to.
     * @param messageNum - The ID of the message being sent. This will help keep track of responses.
     * @param message    - The actual message contents to write.
     * @throws IOException Thrown when an exception occures
     */
    default void writeMessage(OutputStream out, int messageNum, byte[] message) throws IOException
    {
        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        getLogger().trace("Writing message {} {}", messageNum, message);
        /*
         * Writes the message number.
         */
        lengthBuffer.clear();
        lengthBuffer.putInt(messageNum);
        out.write(lengthBuffer.array());
        /*
         * Writes the message length
         */
        /*
         * Wipe out the buffer
         */
        lengthBuffer.clear();
        lengthBuffer.put(new byte[4]);
        lengthBuffer.clear();
        lengthBuffer.putInt(message.length);
        out.write(lengthBuffer.array());
        /*
         * Writes the message
         */
        out.write(message);
        out.flush();
    }
    
    /**
     * Reads a message from the input stream
     *
     * @param in - The input stream to read from.
     * @return Two byte arrays, each with their own value encoded. The first is
     * a 4-byte representation of the message number this response corresponds
     * to. The second array is the message itself. If the stream has been
     * closed, null is returned.
     * @throws IOException Thrown when an exception occurs reading a message.
     */
    default byte[][] readMessage(InputStream in) throws IOException
    {
        int readLength;
        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        byte[] messageNum = new byte[4];
        byte[] message;
        /*
         * Reads the message number.
         */
        readLength = in.read(messageNum);
        if (readLength == 0)
        {
            getLogger().debug("Input stream closed, no more messages.");
            return null;
        }
        /*
         * Reads the message length
         */
        lengthBuffer.clear();
        readLength = in.read(lengthBuffer.array());
        if (readLength == 0)
        {
            getLogger().error("Malformed message, could not get message length.");
            return null;
        }
        message = new byte[lengthBuffer.getInt()];
        /*
         * Writes the message
         */
        readLength = in.read(message);
        if (readLength < message.length)
        {
            getLogger().warn("Message shorter than reported (expected {} bytes, got {} bytes)", message.length, readLength);
        }
        getLogger().trace("Reading message");
        return new byte[][] {messageNum, message};
    }
}
