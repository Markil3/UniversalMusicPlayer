/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * This serves as a central point for controlling the browser process.
 *
 * @author William Hubbard
 * @since 0.1
 */
public class Browser
{
    private static Logger logger = LoggerFactory.getLogger(Browser.class);
    
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
        String args;
        int startExit;
        File browserDir = new File(System.getProperty("user.dir"), "browser");
        
        if (!browserDir.exists())
        {
            browserDir = new File(System.getProperty("user.dir"), "../browser");
        }
        args = " -profile \"" + browserDir.getAbsolutePath() + "/profile\"";
        logger.info("Running on {} {}", os, arch);
//        System.getProperties().entrySet().stream().forEach(entry -> logger.info("{}: {}", entry.getKey(), entry.getValue()));
        if (os.contains("windows"))
        {
            if (arch.contains("64"))
            {
                logger.info("Starting Windows x86_64 browser");
                process = Runtime.getRuntime().exec(new File(browserDir, "windows64/firefox.exe").getAbsolutePath() + args);
            }
            else
            {
                logger.info("Starting Windows x86 browser");
                process = Runtime.getRuntime().exec(new File(browserDir, "windows32/firefox.exe").getAbsolutePath() + args);
            }
        }
        else if (os.contains("linux"))
        {
            if (arch.contains("64"))
            {
                logger.info("Starting Linux x86_64 browser");
                process = Runtime.getRuntime().exec(new File(browserDir, "linux64/firefox").getAbsolutePath() + args);
            }
            else
            {
                logger.info("Starting Linux 86 browser");
                process = Runtime.getRuntime().exec(new File(browserDir, "linux32/firefox").getAbsolutePath() + args);
            }
        }
        if (process == null)
        {
            throw new IOException("Could not find Firefox installation for OS " + os + " " + arch);
        }
    }
    
    /**
     * Tells the browser process to shut down.
     */
    public static void closeBrowser()
    {
        if (process != null)
        {
            logger.info("Destroying browser processes {}, {}", process, process.descendants().toArray(ProcessHandle[]::new));
            process.descendants().forEach(ProcessHandle::destroy);
            process.destroy();
        }
        else
        {
            logger.info("Process already destroyed.");
        }
    }
}
