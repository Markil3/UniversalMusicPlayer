/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import java.io.IOException;
import java.io.InputStream;

import com.intervigil.wave.WaveReader;

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
    private final WaveReader header;
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
        this.header = new WaveReader(this.stream);
        this.header.openWave();
    }

    public WaveReader getHeader()
    {
        return this.header;
    }

    /**
     * Obtains the byte string representation of this header.
     * @return A byte string containing this file's header.
     */
    public byte[] getByteStream()
    {
        try {
            return this.header.getHeaderBytes();
        } catch (IOException e) {
            throw new RuntimeException("Could not recreate header");
        }
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

    @Override
    public void close() throws IOException {
        super.close();
        this.header.closeWaveFile();
    }
}
