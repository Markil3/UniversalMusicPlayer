package edu.regis.universeplayer;

import net.harawata.appdirs.AppDirsFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.Properties;

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

    private static final File propsFile = new File(getConfigDir(), "config" +
            ".prop");
    private static Properties props;

    private static Path[] musicDirs;
    private static Path[] musicIgnoreDirs;

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
                        firefoxDir = new File(firefoxDir.getParentFile()
                                                        .getParent(),
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

    /**
     * Gets the properties file
     *
     * @return The settings contained within the properties object.
     */
    public static Properties getProperties()
    {
        if (props == null)
        {
            Properties defaultProps = new Properties();
            defaultProps.setProperty("musicInclude", System.getProperty("user" +
                    ".home") + File.separator + "Music" + File.pathSeparator + System
                    .getProperty("user.home") + File.separator + "My Music");
            defaultProps.setProperty("musicExclude", "");
            props = new Properties(defaultProps);
            if (propsFile.exists())
            {
                loadProperties();
            }
            else
            {
                saveProperties();
            }
        }
        return props;
    }

    public static void loadProperties()
    {
        try
        {
            props.load(new FileReader(propsFile));
        }
        catch (IOException e)
        {
            logger.error("Error reading configuration file", e);
        }
    }

    public static void saveProperties()
    {
        try
        {
            props.store(new FileWriter(propsFile), "Universal Music " +
                    "Player Properties");
        }
        catch (IOException e)
        {
            logger.error("Error writing configuration file", e);
        }
    }

    /**
     * Obtains folders to scan for music.
     *
     * @return A list of folders to pay attention to.
     */
    public static Path[] getMusicDirs()
    {
        if (musicDirs == null)
        {
            musicDirs =
                    Optional.ofNullable(getProperties().getProperty(
                            "musicInclude"))
                            .map(s -> s.split(File.pathSeparator)).stream()
                            .flatMap(Arrays::stream)
                            .map(String::trim).map(Paths::get)
                            .sorted(Path::compareTo)
                            .toArray(Path[]::new);
        }
        return musicDirs;
    }

    /**
     * Obtains folders to ignore when scanning for music.
     *
     * @return A list of folders to ignore.
     */
    public static Path[] getMusicExcludeDirs()
    {
        if (musicIgnoreDirs == null)
        {
            musicIgnoreDirs =
                    Optional.ofNullable(getProperties().getProperty(
                            "musicExclude"))
                            .map(s -> s.split(File.pathSeparator)).stream()
                            .flatMap(Arrays::stream)
                            .map(String::trim).map(Paths::get)
                            .sorted(Path::compareTo)
                            .toArray(Path[]::new);
        }
        return musicIgnoreDirs;
    }

    /**
     * Checks to see whether a file should be scanned.
     *
     * @param file - The file to scan.
     * @return Whether or not a file is scanned.
     */
    public static boolean scanFolder(Path file)
    {
        for (Path musicIgnoreDir : musicIgnoreDirs)
        {
            if (file.startsWith(musicIgnoreDir) || musicIgnoreDir
                    .endsWith(file))
            {
                /*
                 * Make sure that we don't have an include path that takes
                 * precedence. Working backwards from more specific paths is
                 * more likely to get our results faster.
                 */
                for (int j = musicDirs.length - 1; j >= 0; j--)
                {
                    if (file.startsWith(musicDirs[j]) || musicDirs[j]
                            .endsWith(file))
                    {
                        return true;
                    }
                }
                return false;
            }
        }
        /*
         * Considering that there was nothing in the ignore list, look to
         * make sure that we are allowed to scan it.
         */
        for (Path musicDir : musicDirs)
        {
            if (file.startsWith(musicDir) || musicDir.endsWith(file))
            {
                return true;
            }
        }
        return false;
    }
}
