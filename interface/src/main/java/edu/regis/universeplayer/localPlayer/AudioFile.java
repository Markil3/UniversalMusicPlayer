/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.localPlayer;

import java.io.IOException;
import java.io.InputStream;

import wave.WavHeader;
import wave.WavHeaderReader;

/**
 * Contains information on a WAV audio file, along with a reference to the stream of raw data.
 *
 * @author William Hubbard
 */
public class AudioFile extends InputStream
{
    /**
     * Contains header information.
     */
    private final WavHeaderReader header;
    /**
     * Contains a link to the process reading the audio file.
     */
    private final Process process;
    /**
     * The stream for the actual audio data.
     */
    private InputStream stream;

    /**
     * Creates an audio file from a stream
     *
     * @param stream - The stream to create it from.
     * @throws IOException - Thrown when an error occurs creating the input stream.
     */
    AudioFile(Process stream) throws IOException
    {
        this.process = stream;
        this.stream = stream.getInputStream();
        this.header = new WavHeaderReader(this.stream);
        this.header.read();
    }

    public WavHeader getHeader()
    {
        return this.header.getHeader();
    }

    /**
     * Obtains the byte string representation of this header.
     * @return A byte string containing this file's header.
     */
    public byte[] getByteStream()
    {
        return this.header.getBuf();
    }

    @Override
    public int read() throws IOException
    {
        return this.stream.read();
    }

    @Override
    public int available() throws IOException
    {
        return this.process.isAlive() ? Integer.MAX_VALUE : super.available();
    }
}
