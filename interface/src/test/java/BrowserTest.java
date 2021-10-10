import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;

import edu.regis.universeplayer.PlayerEnvironment;
import edu.regis.universeplayer.browser.Browser;
import edu.regis.universeplayer.browserCommands.QuerySongData;
import edu.regis.universeplayer.data.Album;
import edu.regis.universeplayer.data.InternetSong;
import edu.regis.universeplayer.player.BrowserPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class BrowserTest
{
    private static final Logger logger = LoggerFactory.getLogger(BrowserTest.class);

    @BeforeClass
    public static void setupBrowser()
    {
        HashMap<String, Object> props = new HashMap<>();
        props.put("headless", true);
        PlayerEnvironment.init(props, Collections.emptyList());
    }

    @Test
    public void testPing()
    {
        logger.info("Pinging background");
        logger.info("Background test 1");
        assertEquals(20.0,
                BrowserPlayer.getInstance().ping(20).join(), 0.01);
        logger.info("Background test 2");
        assertEquals(43.1,
                BrowserPlayer.getInstance().ping(43.1).join(), 0.01);
    }

    @Test
    public void testPingForeground() throws MalformedURLException
    {
        logger.info("Pinging foreground");
        logger.info("Forground test 1");
        assertEquals(20.0,
                BrowserPlayer.getInstance()
                             .ping(new URL("https://www.youtube.com/watch?v=FtutLA63Cp8"), 20)
                             .join(), 0.01);
        logger.info("Forground test 2");
        assertEquals(39.5,
                BrowserPlayer.getInstance()
                             .ping(new URL("https://www.youtube" +
                                     ".com/watch?v=FtutLA63Cp8"), 39.5)
                             .join(), 0.01);
        logger.info("Foreground test 3");
        assertEquals(5.1,
                BrowserPlayer.getInstance()
                             .ping(new URL("https://www.youtube.com/watch?v=grMqiZKmUeE"), 5.1)
                             .join(), 0.01);
    }

    @Test
    public void testDataQuery() throws IOException
    {
        logger.info("Retrieving song data");
        InternetSong song = new InternetSong();
        URL url = new URL("https://www.youtube.com/watch?v=cvX4B7GjU6s");
        song.location = url;
        song.title = "First Wave";
        song.artists = new String[]{"Trocadero"};
        song.duration = 224541;
        song.album = new Album();
        song.album.name = "Ghosts That Linger";
        song.album.year = 2009;
        song.album.artists = new String[]{"Rooster Teeth Records / Trocadero"};

        assertEquals(song,
                BrowserPlayer.getInstance()
                             .getSongData(url).join());
    }
}
