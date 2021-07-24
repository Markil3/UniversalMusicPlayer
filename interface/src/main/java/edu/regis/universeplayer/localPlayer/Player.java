/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */
package edu.regis.universeplayer.localPlayer;

import com.intervigil.wave.WaveReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

/**
 * This allows for the control of playback of files on the local file system
 *
 * @author William Hubbard
 * @version 0.1
 */
public class Player
{
    static
    {
        System.loadLibrary("player");
    }

    private int currentId;

    private AudioFile currentFile;

    /**
     * Sets the file currently being used.
     *
     * @param file - The current audio file.
     */
    public void setCurrentFile(AudioFile file)
    {
        WaveReader header = file.getHeader();
        this.currentFile = file;
        // TODO - Ensure that bytes per sample and bits per sample don't bother things
        this.currentId = this.setCurrentFile(this.currentFile, (short) header.getChannels(), (short) header
                .getPcmFormat(), header.getSampleRate());
    }

    /**
     * Updates the file currently being played.
     *
     * @param stream - A reference to the audio data stream.
     * @param numChannels - The number of audio channels contained in the file.
     * @param bitsPerSample - The number if bits in every sample.
     * @param sampleRate - How many samples need to play every second.
     * @return An ID for the current song.
     */
    private native int setCurrentFile(InputStream stream, short numChannels, short bitsPerSample, int sampleRate);

    /**
     * Saves the current file to
     *
     * @param file - The file to save to.
     */
    public native void save(String file);

    /**
     * Saves the current file as a WAVE file somewhere else. This method is meant for testing only.
     *
     * @param file - The file to save to.
     */
    public void saveJava(String file)
    {
        try (FileOutputStream out = new FileOutputStream(file))
        {
            out.write(this.getAudioFile().getByteStream());
            byte[] buffer = new byte[256];
            int read = this.getAudioFile().read(buffer);
            while (read > 0)
            {
                out.write(buffer, 0, read);
                read = this.getAudioFile().read(buffer);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Obtains the current file.
     *
     * @return The current audio file we are working with.
     */
    public AudioFile getAudioFile()
    {
        return this.currentFile;
    }

    /**
     * Plays the current audio file.
     */
    public native void play();

    /**
     * Pauses playback for the current audio file.
     */
    public native void pause();

    /**
     * Checks to see whether the current audio file is paused.
     *
     * @return True if there is an audio file present and it is paused, false otherwise.
     */
    public native boolean isPaused();

    /**
     * Obtains an input stream for the requested file.
     *
     * @param file - The file to read
     * @return An raw stream for the file
     * @throws FileNotFoundException - Thrown should the file not exist.
     * @throws IOException           - Thrown should an error occur when reading the file
     */
    public static AudioFile getAudioStream(File file) throws IOException
    {
        return new AudioFile(convertFile(file));
    }

    /**
     * Converts any audio file to a stream containing WAV audio file data (courtesy of FFMPEG).
     *
     * @param file - The file to convert
     * @return An input stream containing the file data
     * @throws FileNotFoundException - Thrown should the file not exist.
     * @throws IOException           - Thrown should an error occur when reading the file
     */
    protected static Process convertFile(File file) throws FileNotFoundException, IOException
    {
        LinkedList<String> args = new LinkedList<>();
        if (!file.isFile())
        {
            throw new FileNotFoundException("Must provide a file");
        }
        args.add("ffmpeg");
        args.add("-hide_banner");
        args.add("-loglevel");
        args.add("error");
        args.add("-y");
        args.add("-i");
        args.add(file.getAbsolutePath());
        args.add("-f");
        args.add("wav");
        args.add("pipe:1");
        return Runtime.getRuntime().exec(args.toArray(new String[args.size()]));
    }

    public static void main(String[] args)
    {
        byte[] buffer = new byte[200];
        int len;
        File file = new File(args[0]);
        try (AudioFile stream = getAudioStream(new File(args[0])))
        {
            Player player = new Player();
            player.setCurrentFile(stream);
            player.save(args[0] + ".wav");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        // Compare JNI vs Java
        try (AudioFile stream = getAudioStream(new File(args[0])))
        {
            Player player = new Player();
            player.setCurrentFile(stream);
            player.saveJava(args[0] + ".orig.wav");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
