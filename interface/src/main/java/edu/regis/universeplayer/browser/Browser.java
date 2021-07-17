/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browser;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This serves as a central point for controlling the browser process.
 *
 * @author William Hubbard
 * @since 0.1
 */
public class Browser
{
    private static Process process;

    /**
     * Utility method for launching a browser instance
     *
     * @throws IOException - Thrown if there is a problem launching the browser.
     */
    public static void launchBrowser() throws IOException
    {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String args = " -profile browser/profile";
        if (process != null && process.isAlive())
        {
            process.destroy();
        }

        if (os.contains("windows"))
        {
            setupWindows();
            process = Runtime.getRuntime().exec("browser/windows/FirefoxPortable.exe" + args);
        }
        else if (os.contains("linux"))
        {
            setupLinux();
            if (arch.contains("64"))
            {
                process = Runtime.getRuntime().exec("./browser/linux64/firefox" + args);
            }
            else
            {
                process = Runtime.getRuntime().exec("./browser/linux32/firefox" + args);
            }
        }

        System.out.println(os);
    }

    /**
     * Performs first-time setup for Windows applications.
     */
    private static void setupWindows()
    {
        try
        {
            Runtime.getRuntime()
                   .exec("REG ADD HKEY_CURRENT_USER\\SOFTWARE\\Mozilla\\NativeMessagingHosts /v universal_music /d \"" + System
                           .getProperty("user.dir") + "\\bin\\interface.bat\" ");
        }
        catch (IOException e)
        {
            System.err.println("Could not set Windows registry key.");
            e.printStackTrace();
        }
    }

    /**
     * Performs first-time setup for OSX applications.
     */
    private static void setupMac()
    {
        Gson gson = new Gson();
        try (JsonWriter writer = gson.newJsonWriter(new FileWriter(new File(System
                .getProperty("user.home"), "Library/Application Support/Mozilla/NativeMessagingHosts/universal_music.json"))))
        {
            writeManifest(writer, System.getProperty("user.dir") + "/bin/interface");
        }
        catch (IOException e)
        {
            System.err.println("Could not set Mac application manifest.");
            e.printStackTrace();
        }
    }

    /**
     * Performs first-time setup for Linux applications.
     */
    private static void setupLinux()
    {
        Gson gson = new Gson();
        try (JsonWriter writer = gson.newJsonWriter(new FileWriter(new File(System
                .getProperty("user.home"), ".mozilla/native-messaging-hosts/universal_music.json"))))
        {
            writeManifest(writer, System.getProperty("user.dir") + "/bin/interface");
        }
        catch (IOException e)
        {
            System.err.println("Could not set Linux application manifest.");
            e.printStackTrace();
        }
    }

    private static void writeManifest(JsonWriter writer, String path) throws IOException
    {
        writer.beginObject();

        writer.name("name");
        writer.value("universal_music");
        writer.name("description");
        writer.value("Universal Music Player");
        writer.name("path");
        writer.value(path);
        writer.name("type");
        writer.value("stdio");

        writer.name("allowed_extensions");
        writer.beginArray();
        writer.value("universal_music@regis.edu");
        writer.endArray();

        writer.endObject();
    }

    /**
     * Tells the browser process to shut down.
     */
    public static void closeBrowser()
    {
        if (process != null && process.isAlive())
        {
            process.destroy();
        }
    }
}
