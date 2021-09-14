package edu.regis.universeplayer;

import net.harawata.appdirs.AppDirsFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ConfigManager
{
    private static final Logger logger = LoggerFactory
            .getLogger(ConfigManager.class);

    private static File dataDir;
    private static File commDir;
    private static File configDir;
    /**
     * Obtains the data storage directory for the application, creating it if
     * needed.
     *
     * @return The data storage directory.
     */
    public static File getDataDir()
    {
        if (dataDir == null)
        {
            dataDir = new File(AppDirsFactory.getInstance()
                                             .getUserDataDir("universalmusic", null, null, true));
            if (!dataDir.exists())
            {
                if (!dataDir.mkdir())
                {
                    logger.error("Could not create data directory {}", dataDir);
                }
            }
        }
        return dataDir;
    }

    /**
     * Obtains the configuration directory for the application, creating it if
     * needed.
     *
     * @return The configuration directory.
     */
    public static File getConfigDir()
    {
        if (configDir == null)
        {
            configDir = new File(AppDirsFactory.getInstance()
                                               .getUserConfigDir("universalmusic", null, null, true));
            if (!configDir.exists())
            {
                if (!configDir.mkdir())
                {
                    logger.error("Could not create configuration directory {}", configDir);
                }
            }
        }
        return configDir;
    }

    /**
     * Obtains the directory for memory mapped files
     *
     * @return The communications storage directory.
     */
    public static File getCommDir()
    {
        if (commDir == null)
        {
            commDir = new File(AppDirsFactory.getInstance()
                                             .getSharedDir("universalmusic", null, null), "comm");
            if (!commDir.getParentFile().exists())
            {
                if (!commDir.getParentFile().mkdir())
                {
                    logger.error("Could not create shared directory {}", commDir
                            .getParent());
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
