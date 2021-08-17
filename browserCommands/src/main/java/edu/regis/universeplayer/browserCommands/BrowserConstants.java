/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

import net.harawata.appdirs.AppDirsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class BrowserConstants
{
    private static final Logger logger = LoggerFactory.getLogger(BrowserConstants.class);
    /**
     * The IP that the connection will be hosted on.
     */
    public static final String IP = "localhost";
    /**
     * The port both processes use for connection.
     */
    public static final int PORT = 3000;
    
    private static File commDir;
    
    /**
     * Obtains the directory for memory mapped files
     *
     * @return The communications storage directory.
     */
    public static File getCommDir()
    {
        if (commDir == null)
        {
            commDir = new File(AppDirsFactory.getInstance().getSharedDir("universalmusic", null, null), "comm");
            if (!commDir.getParentFile().exists())
            {
                if (!commDir.getParentFile().mkdir())
                {
                    logger.error("Could not create shared directory {}", commDir.getParent());
                }
            }
            if (!commDir.exists())
            {
                if (!commDir.mkdir())
                {
                    logger.error("Could not create data directory {}", commDir);
                }
            }
        }
        return commDir;
    }
}
