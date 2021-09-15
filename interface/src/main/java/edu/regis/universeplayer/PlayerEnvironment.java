package edu.regis.universeplayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Scanner;
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
        parseArgs(args, ops, params);

        if (ops.containsKey("h") || ops.containsKey("help"))
        {
            printHelp();
            return;
        }

        if (!connectToServer(args))
        {
            init(ops, params);
        }
    }

    /**
     * Forwards command arguments to the server.
     *
     * @param args - The argument list to forward.
     * @return True if the server exists, false if not.
     */
    private static boolean connectToServer(String[] args)
    {
        Socket socket;
        try
        {
            socket = new Socket("localhost", ConfigManager.PORT);
            try (PrintStream out = new PrintStream(socket.getOutputStream()))
            {
                boolean quote;
                for (String arg : args)
                {
                    quote = arg.contains(" ");
                    if (quote)
                    {
                        out.print('"');
                    }
                    out.print(arg);
                    if (quote)
                    {
                        out.print('"');
                    }
                    out.print(' ');
                }
                out.println();
                try (Scanner in = new Scanner(socket.getInputStream()))
                {
                    while (in.hasNextLine())
                    {
                        System.out.println(in.nextLine());
                    }
                }
                out.print(1);
            }
            return true;
        }
        catch (IOException e)
        {
            logger.debug("Couldn't connect to server", e);
            return false;
        }
    }

    private static void init(HashMap<String, Object> ops,
                             ArrayList<String> params)
    {
        /*
         * Add this just in case of a crash or something. It won't work if the
         * program is forcibly terminated by the OS, but it could be helpful
         * otherwise.
         */
        logger.info("Starting application {} {}", params, ops);

        InstanceConnector connector = new InstanceConnector();
        new Thread(connector).start();

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
                else if (queue1.getCurrentSong() != null)
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
        runArguments(ops, params, System.out);

        Runtime.getRuntime().addShutdownHook(new Thread(connector::stop));
    }

    /**
     * Converts an array of string arguments into a map of options and extra
     * parameters.
     *
     * @param args    - The arguments to parse.
     * @param options - The map to dump the options into.
     * @param params  - The list to dump the other parameters into.
     */
    public static void parseArgs(String[] args,
                                 Map<String, Object> options,
                                 List<String> params)
    {
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
                        options.put(args[i].substring(j, j + 1), null);
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
                options.put(key, value);
            }
            else
            {
                params.add(args[i]);
            }
        }
    }

    /**
     * Runs command-line arguments
     *
     * @param options - Run options
     * @param params  - A list of song terms to search for and enqueue.
     * @param out     - Program output. While this may simply be System .out,
     *                this is just as likely going to be outputting to another
     *                program instance.
     */
    public static void runArguments(Map<String, Object> options,
                                    List<String> params, PrintStream out)
    {
        try
        {
            out.println(options.toString());
            out.println(params.toString());
            for (String op : options.keySet())
            {
                switch (op)
                {
                case "play" -> {
                    PlayerManager.getPlayers().play();
                    out.println("Playing");
                }
                case "pause" -> {
                    PlayerManager.getPlayers().pause();
                    out.println("Pausing");
                }
                case "toggle", "t" -> {
                    PlayerManager.getPlayers().toggle();
                    out.println("Toggling playback");
                }
                case "seek" -> {
                    PlayerManager.getPlayers()
                                 .seek((int) options.get("seek"));
                    out.println("Seeking");
                }
                case "clear", "c" -> {
                    out.println("Clearing");
                    Queue.getInstance().clear();
                }
                case "next", "n" -> {
                    Integer number = null;
                    if (options.get("next") instanceof Integer)
                    {
                        number = (Integer) options.get("next");
                    }
                    else if (options.get("n") instanceof Integer)
                    {
                        number = (Integer) options.get("n");
                    }
                    if (number == null)
                    {
                        Queue.getInstance().skipNext();
                    }
                    else
                    {
                        Queue.getInstance().skipToSong(Queue.getInstance()
                                                            .getCurrentIndex() + number);
                    }
                    out.println("Next Song");
                }
                case "prev", "p" -> {
                    Integer number = null;
                    if (options.get("prev") instanceof Integer)
                    {
                        number = (Integer) options.get("prev");
                    }
                    else if (options.get("p") instanceof Integer)
                    {
                        number = (Integer) options.get("p");
                    }
                    if (number == null)
                    {
                        Queue.getInstance().skipNext();
                    }
                    else
                    {
                        Queue.getInstance().skipToSong(Queue.getInstance()
                                                            .getCurrentIndex() - number);
                    }
                    out.println("Clearing");
                }
                case "skip" -> {
                    Integer number = null;
                    if (options.get("skip") instanceof Integer)
                    {
                        number = (Integer) options.get("skip");
                    }
                    if (number == null)
                    {
                        Queue.getInstance().skipNext();
                    }
                    else
                    {
                        Queue.getInstance().skipToSong(number);
                    }
                    out.println("Clearing");
                }
                case "status" -> {
                    QueryFuture<PlaybackStatus> status =
                            PlayerManager.getPlayers().getStatus();
                    try
                    {
                        out.println(status.get());
                    }
                    catch (InterruptedException | ExecutionException e)
                    {
                        e.printStackTrace(out);
                    }
                }
                case "song" -> {
                    Song song = PlayerManager.getPlayers().getCurrentSong();
                    out.println(song.toString());
                }
                case "queue" -> {
                    for (Song song : Queue.getInstance())
                    {
                        out.print(" ");
                        if (song == PlayerManager.getPlayers().getCurrentSong())
                        {
                            out.print("*");
                        }
                        else
                        {
                            out.print(" ");
                        }
                        out.print(" ");
                        out.println(song.toString());
                    }
                }
                }
            }
            HashMap<Song, Integer> matchMap = new HashMap<>();
            for (String param : params)
            {
                String finalParam = param.toLowerCase();
                SongProvider.INSTANCE.getSongs().forEach(song -> {
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
                        if (song.album.name != null && song.album.name
                                .toLowerCase()
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
                        out.printf("%d matches for %s and %s\n", matches,
                                finalParam, song);
                        matchMap.put(song, matches);
                    }
                });
            }
            if (matchMap.size() > 0)
            {
                List<Song> songs = matchMap.entrySet().stream().sorted(Map.Entry
                        .comparingByValue()).filter(entry -> (entry
                        .getValue() / (float) params.size()) >= 0.65F)
                                           .map(entry -> {
                                               out.printf("%s: %d / %d (%f)\n",
                                                       entry.getKey(),
                                                       entry.getValue(),
                                                       params.size(),
                                                       entry.getValue() / (float) params
                                                               .size());
                                               return entry.getKey();
                                           })
                                           .collect(Collectors.toList());
                Collections.reverse(songs);
                out.println("Playing " + songs.toString());
                Queue.getInstance().addAll(songs);
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace(out);
        }
    }

    public static void printHelp()
    {
        System.out.println("Universal Music Player");
        System.out.println("----------------------");
        System.out.println("CLI Arguments");
        System.out.println("\t--headless");
        System.out.println("\t\tRuns the player without a GUI.");
        System.out.println("\t--play");
        System.out.println("\t\tStarts playback");
        System.out.println("\t--pause");
        System.out.println("\t\tPauses playback");
        System.out.println("\t--toggle, -t");
        System.out.println("\t\tToggles playback");
        System.out.println("\t--seek <time>");
        System.out.println("\t\tSeeks the player to the provided time, in " +
                "seconds");
        System.out.println("\t--clear, -c");
        System.out.println("\t\tClears the queue before adding songs");
        System.out.println("\t--next, -n [skipBy]");
        System.out.println("\t\tSkips to the next song by skipBy songs. " +
                "Defaults to 1.");
        System.out.println("\t--prev, -p [skipBy]");
        System.out.println("\t\tSkips to the previous song by skipBy songs. " +
                "Defaults to 1.");
        System.out.println("\t--skip [skipTo]");
        System.out.println("\t\tSkips to the requested song in the queue. " +
                "Defaults to the next song.");
        System.out.println("\t--status");
        System.out.println("\t\tObtains the playback status");
        System.out.println("\t--song");
        System.out.println("\t\tObtains the current song");
        System.out.println("\t--queue");
        System.out.println("\t\tGets the queue");
        System.out.println("\t--help, -h");
        System.out.println("\t\tPrints this help message");
    }
}
