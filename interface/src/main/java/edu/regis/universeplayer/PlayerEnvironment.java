package edu.regis.universeplayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import edu.regis.universeplayer.browserCommands.QueryFuture;
import edu.regis.universeplayer.data.Queue;
import edu.regis.universeplayer.data.Song;
import edu.regis.universeplayer.data.SongProvider;
import edu.regis.universeplayer.gui.Interface;
import edu.regis.universeplayer.player.PlayerManager;

/**
 * A centralized spot to link up all of the components.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class PlayerEnvironment
{
    private static final Logger logger = LoggerFactory
            .getLogger(PlayerEnvironment.class);
    private static final ResourceBundle langs = ResourceBundle
            .getBundle("lang.interface", Locale.getDefault());

    /**
     * Initializes the various components, setting up listeners as needed.
     */
    public static void main(String[] args)
    {
        LinkedHashMap<String, Object> ops = new LinkedHashMap<>();
        ArrayList<String> params = new ArrayList<>();
        int equals = -1;
        Object value;
        String key;
        String valStr;
        int i, j, l, l2;
        for (i = 0, l = args.length; i < l; i++)
        {
            valStr = null;
            if (args[i].startsWith("-"))
            {
                if (args[i].startsWith("--"))
                {
                    equals = args[i].indexOf('=');
                    if (equals != -1)
                    {
                        valStr = args[i].substring(equals + 1);
                    }
                    else
                    {
                        equals = args[i].length();
                        valStr = null;
                    }
                    key = args[i].substring(2, equals);
                }
                else
                {
                    for (j = 1, l2 = args[i].length() - 1; j < l2; j++)
                    {
                        ops.put(args[i].substring(j, j + 1), null);
                    }
                    key = args[i].substring(j, j + 1);
                }
                if (valStr == null && i < args.length - 1 && !args[i + 1]
                        .startsWith("-"))
                {
                    valStr = args[++i];
                }
                if (valStr != null)
                {
                    if (Pattern.matches("\\d+", valStr))
                    {
                        value = Integer.parseInt(valStr);
                    }
                    else if (Pattern.matches("\\d*\\.\\d+", valStr))
                    {
                        value = Double.parseDouble(valStr);
                    }
                    else
                    {
                        value = valStr;
                    }
                }
                else
                {
                    value = true;
                }
                ops.put(key, value);
            }
            else
            {
                params.add(args[i]);
            }
        }

        if (ops.containsKey("h") || ops.containsKey("help"))
        {
            printHelp();
            return;
        }

        /*
         * Add this just in case of a crash or something. It won't work if the
         * program is forcibly terminated by the OS, but it could be helpful
         * otherwise.
         */
        logger.info("Starting application {} {}", params, ops);

        Queue queue = Queue.getInstance();
        PlayerManager playback = PlayerManager.getPlayers();
        queue.addSongChangeListener(queue1 -> {
            QueryFuture<Void> command = PlayerManager.getPlayers()
                                                     .stopSong();
            try
            {
                if (command != null && !command.getConfirmation()
                                               .wasSuccessful())
                {
                    if (Interface.getInstance() != null)
                    {
                        JOptionPane
                                .showMessageDialog(Interface.getInstance(),
                                        command.getConfirmation()
                                               .getError(),
                                        command.getConfirmation()
                                               .getMessage(),
                                        JOptionPane.ERROR_MESSAGE);
                    }
                    logger.error("Could not run command",
                            command.getConfirmation().getError());
                }
                else
                {
                    command =
                            PlayerManager.getPlayers()
                                         .playSong(queue1.getCurrentSong());
                    if (!command.getConfirmation().wasSuccessful())
                    {
                        if (Interface.getInstance() != null)
                        {
                            JOptionPane.showMessageDialog(Interface
                                            .getInstance(),
                                    command.getConfirmation().getError(),
                                    command.getConfirmation().getMessage(),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                        logger.error("Could not run command",
                                command.getConfirmation().getError());
                    }
                    else
                    {
                    }
                }
            }
            catch (ExecutionException | InterruptedException e)
            {
                logger.error("Could not get current playback status", e);
                if (Interface.getInstance() != null)
                {
                    JOptionPane
                            .showMessageDialog(Interface.getInstance(), e
                                            .getMessage()
                                    , langs
                                            .getString(
                                                    "error.command"), JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        playback.addPlaybackListener(status -> {
            switch (status.getInfo().getStatus())
            {
            case FINISHED -> Queue.getInstance().skipNext();
            }
        });

        if (!ops.containsKey("headless"))
        {
            Interface inter = new Interface();
            inter.setSize(700, 500);
            SongProvider.INSTANCE.addUpdateListener(inter);
            inter.setVisible(true);
        }

        /*
         * Open up the relevant songs
         */
        HashMap<Song, Integer> matchMap = new HashMap<>();
        for (String param : params)
        {
            String finalParam = param.toLowerCase();
            SongProvider.INSTANCE.getSongs().stream().forEach(song -> {
                Integer matches = matchMap.get(song);
                if (matches == null)
                {
                    matches = 0;
                }
                if (song.title != null && song.title.toLowerCase()
                                                    .contains(finalParam))
                {
                    matches++;
                }
                if (song.album != null)
                {
                    if (song.album.name != null && song.album.name.toLowerCase()
                                                                  .contains(finalParam))
                    {
                        matches++;
                    }
                    if (song.album.artists != null && song.album.artists.length > 0)
                    {
                        for (String artist : song.album.artists)
                        {
                            if (artist.toLowerCase().contains(finalParam))
                            {
                                matches++;
                            }
                        }
                    }
                }
                if (song.artists != null && song.artists.length > 0)
                {
                    for (String artist : song.artists)
                    {
                        if (artist.toLowerCase().contains(finalParam))
                        {
                            matches++;
                        }
                    }
                }
                if (matches > 0)
                {
                    matchMap.put(song, matches);
                }
            });
        }
        if (matchMap.size() > 0)
        {
            List<Song> songs = matchMap.entrySet().stream().sorted(Map.Entry
                    .comparingByValue()).filter(entry -> (entry
                    .getValue() / (float) params.size()) >= 0.65F)
                                       .map(Map.Entry::getKey)
                                       .collect(Collectors.toList());
            System.out.println("Playing " + songs.toString());
            Collections.reverse(songs);
            Queue.getInstance().addAll(songs);
        }
        for (String op : ops.keySet())
        {
            switch (op)
            {
            case "play" -> PlayerManager.getPlayers().play();
            case "pause" -> PlayerManager.getPlayers().pause();
            case "toggle", "t" -> PlayerManager.getPlayers().toggle();
            case "seek" -> PlayerManager.getPlayers()
                                        .seek((int) ops.get("seek"));
            }
        }
    }

    private static void printHelp()
    {
        System.out.println("Universal Music Player");
        System.out.println("----------------------");
        System.out.println("CLI Arguments");
        System.out.println("\t--headless");
        System.out.println("\t\tRuns the player without a GUI.");
        System.out.println("\t--help, -h");
        System.out.println("\t\tPrints this help message");
    }
}
