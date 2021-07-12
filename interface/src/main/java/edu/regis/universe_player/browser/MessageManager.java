/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universe_player.browser;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.util.Scanner;

/**
 * This class will send messages to the
 */
public class MessageManager implements Closeable
{
    private final JsonReader in;
    private final JsonWriter out;
    private final Gson gson;

    public MessageManager() throws IOException
    {
        this.gson = new Gson();
        this.in = this.gson.newJsonReader(new InputStreamReader(System.in));
        this.out = this.gson.newJsonWriter(new OutputStreamWriter(System.out));
    }

    /**
     * Retrieves an object from the browser.
     *
     * @return - A parsed object. What it is depends on the browser.
     */
    public Object getMessage() throws IOException
    {
        Object value = null;
        this.in.beginObject();
        while (this.in.hasNext())
        {
            switch (this.in.nextName())
            {
            case "response":
                switch (this.in.peek())
                {
                case STRING:
                    value = this.in.nextString();
                    break;
                case NUMBER:
                    value = this.in.nextDouble();
                    break;
                case BOOLEAN:
                    value = this.in.nextBoolean();
                    break;
                case NULL:
                    this.in.nextNull();
                    break;
                default:
                    throw new IOException("Unexpected JSON type" + this.in.peek());
                }
                break;
            }
        }
        this.in.endObject();

        return value;
    }

    /**
     * Sends a string message to the browser.
     *
     * @param message - The message to send.
     */
    public void writeMessage(String message)
    {
        String value = null;
        String type = null;
        try
        {
            this.out.beginObject();
            this.out.name("message");
            this.out.value(message);
            this.out.endObject();
        }
        catch (IOException e)
        {
        }
    }

    /**
     * Pings the browser.
     */
    public void ping()
    {
        try
        {
            this.out.beginObject();
            this.out.name("command");
            this.out.value("ping");
            this.out.endObject();
        }
        catch (IOException e)
        {
        }
    }

    @Override
    public void close() throws IOException
    {
        this.in.close();
        this.out.close();
    }
}
