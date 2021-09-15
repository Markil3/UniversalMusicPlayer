package edu.regis.universeplayer;

import net.harawata.appdirs.AppDirsFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ConfigManager
{
    /**
     * The port the player will listen for connecting instances on.
     */
    public static final int PORT = 3001;

    private static final Logger logger = LoggerFactory
            .getLogger(ConfigManager.class);

    private static File dataDir;
    private static File commDir;
    private static File configDir;
    private static File appDir;
    private static File firefoxDir;

    /**
     * Obtains the install directory for the application.
     *
     * @return The application storage directory.
     */
    public static File getAppDir()
    {
        if (appDir == null)
        {
            appDir = new File(System.getProperty("java.class" +
                    ".path").split(File.pathSeparator)[0]);
            appDir = appDir.getParentFile();
            firefoxDir = new File(appDir, "firefox");
            if (!firefoxDir.exists())
            {
                firefoxDir = new File(appDir.getParent(), "firefox");
                if (firefoxDir.exists())
                {
                    appDir = appDir.getParentFile();
                }
                else
                {
                    firefoxDir = new File(System.getProperty("user.dir"),
                            "firefox");
                    if (firefoxDir.exists())
                    {
                        appDir = new File(System.getProperty("user.dir"));
                    }
                    else
                    {
                        firefoxDir = new File(firefoxDir.getParentFile().getParent(),
                                "firefox");
                        if (firefoxDir.exists())
                        {
                            appDir = firefoxDir.getParentFile();
                        }
                    }
                }
            }
        }
        return appDir;
    }

    /**
     * Obtains the install directory the firefox installation..
     *
     * @return The firefox storage directory.
     */
    public static File getFirefoxDir()
    {
        if (firefoxDir == null)
        {
            getAppDir();
        }
        return firefoxDir;
    }

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
