package edu.regis.universeplayer;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import edu.regis.universeplayer.data.AlbumProvider;
import edu.regis.universeplayer.data.CompiledSongProvider;
import edu.regis.universeplayer.data.DefaultAlbumProvider;
import edu.regis.universeplayer.data.InternetSongProvider;
import edu.regis.universeplayer.data.LocalSongProvider;
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

    private static Options OPTIONS;

    private static AlbumProvider ALBUMS_INSTANCE;
    private static SongProvider<?> SONGS_INSTANCE;

    public static AlbumProvider getAlbums()
    {
        return ALBUMS_INSTANCE;
    }

    public static SongProvider<?> getSongs()
    {
        return SONGS_INSTANCE;
    }

    /**
     * Initializes the various components, setting up listeners as needed.
     */
    public static void main(String[] args)
    {
        Options ops = setupCLIArgs();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;//not a good practice, it serves it purpose

        try
        {
            cmd = parser.parse(ops, args);
        }
        catch (ParseException e)
        {
            System.err.println(e.getMessage());
            printHelp();

            System.exit(1);
        }

        if (cmd.hasOption("help"))
        {
            printHelp();
            System.exit(0);
        }

        /*
         * Attempts to forward the args to the running instance, or start a new
         * instance if there is no instance running.
         */
        if (!connectToServer(cmd))
        {
            init(cmd);
        }
    }

    /**
     * Forwards command arguments to the server.
     *
     * @param args - The argument list to forward.
     * @return True if the server exists, false if not.
     */
    private static boolean connectToServer(CommandLine args)
    {
        String data;
        Socket socket;
        boolean success = false;
        try
        {
            socket = new Socket("localhost", ConfigManager.PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            Scanner in = new Scanner(socket.getInputStream());
            logger.debug("Connecting to remote");
            out.writeObject(args);
            logger.debug("Waiting for output");
            while (in.hasNextLine())
            {
                data = in.nextLine();
                if (data.equals("END"))
                {
                    break;
                }
                System.out.println(data);
            }
            success = true;
            logger.debug("Finished reading return output.");
            out.writeInt(-1);
        }
        catch (IOException e)
        {
            logger.debug("Couldn't connect to server", e);
        }
        return success;
    }

    public static void init(CommandLine cmd)
    {
        /*
         * Add this just in case of a crash or something. It won't work if the
         * program is forcibly terminated by the OS, but it could be helpful
         * otherwise.
         */
        logger.info("Starting application {}", cmd);

        InstanceConnector connector = new InstanceConnector();
        new Thread(connector).start();

        ALBUMS_INSTANCE = new DefaultAlbumProvider();
        SONGS_INSTANCE =
                new CompiledSongProvider(new LocalSongProvider(ALBUMS_INSTANCE),
                        new InternetSongProvider(ALBUMS_INSTANCE));

        Queue queue = Queue.getInstance();
        PlayerManager playback = PlayerManager.getPlayers();
        queue.addSongChangeListener(queue1 -> ForkJoinPool.commonPool().submit(() ->
        {
            ForkJoinTask<Void> command = PlayerManager.getPlayers()
                    .stopSong();
            if (command != null)
            {
                command.join();
                if (command.isCompletedAbnormally())
                {
                    logger.error("Could not run command",
                            command.getException());
                    if (Interface.getInstance() != null)
                    {
                        JOptionPane
                                .showMessageDialog(Interface.getInstance(),
                                        command.getException(),
                                        command.getException().getMessage(),
                                        JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            if (queue1.getCurrentSong() != null)
            {
                command =
                        PlayerManager.getPlayers()
                                .playSong(queue1.getCurrentSong());
                command.join();
                if (command.isCompletedAbnormally())
                {
                    logger.error("Could not run command",
                            command.getException());
                    if (Interface.getInstance() != null)
                    {
                        JOptionPane
                                .showMessageDialog(Interface.getInstance(),
                                        command.getException(),
                                        command.getException().getMessage(),
                                        JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }));
        playback.addPlaybackListener(status ->
        {
            logger.info("Receiving {} from {}", status.getInfo(), status
                    .getSource());
            if (status.getInfo().getStatus() == PlaybackStatus.FINISHED)
            {
                Queue.getInstance().skipNext();
            }
        });

        if (!cmd.hasOption("headless"))
        {
            Interface inter = new Interface();
            inter.setSize(700, 500);
            SONGS_INSTANCE.addUpdateListener(inter);
            inter.setVisible(true);
        }

        /*
         * Open up the relevant songs
         */
        runArguments(cmd, System.out);

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            connector.stop();
            PlayerManager.getPlayers().shutdownPlayers();
        }));
    }

    private static Options setupCLIArgs()
    {
        if (OPTIONS == null)
        {
            OPTIONS = new Options();
            /*
             * Launch options
             */
            Option headless = Option.builder().longOpt("headless")
                    .desc("Runs the player without a GUI.")
                    .build();
            OPTIONS.addOption(headless);
            Option help = Option.builder("h").longOpt("help")
                    .desc("Prints this help message.")
                    .build();
            OPTIONS.addOption(help);

            /*
             * Playback options
             */
            Option play = Option.builder().longOpt("play")
                    .desc("Starts playback.")
                    .build();
            OPTIONS.addOption(play);
            Option pause = Option.builder().longOpt("pause")
                    .desc("Starts playback.")
                    .build();
            OPTIONS.addOption(pause);
            Option toggle = Option.builder("t").longOpt("toggle")
                    .desc("Toggles playback.")
                    .build();
            OPTIONS.addOption(toggle);
            Option seek = Option.builder().longOpt("seek")
                    .hasArg().argName("time").desc("Seeks the player to the provided time, in seconds.")
                    .build();
            OPTIONS.addOption(seek);
            Option clear = Option.builder("c").longOpt("clear")
                    .desc("Clears the queue before adding songs.")
                    .build();
            OPTIONS.addOption(clear);
            Option next = Option.builder("n").longOpt("next")
                    .hasArg().optionalArg(true).argName("skipBy")
                    .desc("Skips to the next song by skipBy songs. Defaults to 1.")
                    .build();
            OPTIONS.addOption(next);
            Option prev = Option.builder("p").longOpt("prev")
                    .hasArg().optionalArg(true).argName("skipBy")
                    .desc("Skips to the previous song by skipBy songs. Defaults to 1.")
                    .build();
            OPTIONS.addOption(prev);
            Option skip = Option.builder().longOpt("skip")
                    .hasArg().optionalArg(true).argName("skipTo")
                    .desc("Skips to the requested song in the queue. Defaults to the next song.")
                    .build();
            OPTIONS.addOption(skip);
            Option status = Option.builder().longOpt("status")
                    .desc("Obtains the playback status.")
                    .build();
            OPTIONS.addOption(status);
            Option queue = Option.builder().longOpt("queue")
                    .desc("Gets the queue.")
                    .build();
            OPTIONS.addOption(queue);

            Option search = Option.builder().longOpt("search")
                    .desc("Searches the library for songs matching the arguments, instead of enqueuing them.")
                    .build();
            OPTIONS.addOption(search);
        }
        return OPTIONS;
    }

    /**
     * Runs command-line arguments
     *
     * @param cmd - A list of song terms to search for and enqueue.
     * @param out - Program output. While this may simply be System .out, this
     *            is just as likely going to be outputting to another program
     *            instance.
     */
    public static void runArguments(CommandLine cmd, PrintStream out)
    {
        try
        {
            out.println(cmd.toString());
            for (Option op : cmd.getOptions())
            {
                switch (op.getLongOpt())
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
                            .seek(Float.parseFloat(op.getValue()));
                    out.println("Seeking");
                }
                case "clear" -> {
                    out.println("Clearing");
                    Queue.getInstance().clear();
                }
                case "next" -> {
                    String number = op.getValue();
                    if (number == null)
                    {
                        Queue.getInstance().skipNext();
                    }
                    else
                    {
                        Queue.getInstance().skipToSong(Queue.getInstance()
                                .getCurrentIndex() + Integer.parseInt(number));
                    }
                    out.println("Next Song");
                }
                case "prev" -> {
                    String number = op.getValue();
                    if (number == null)
                    {
                        Queue.getInstance().skipNext();
                    }
                    else
                    {
                        Queue.getInstance().skipToSong(Queue.getInstance()
                                .getCurrentIndex() - Integer.parseInt(number));
                    }
                    out.println("Previous Song");
                }
                case "skip" -> {
                    String number = op.getValue();
                    if (number == null)
                    {
                        Queue.getInstance().skipNext();
                    }
                    else
                    {
                        Queue.getInstance().skipToSong(Integer.parseInt(number));
                    }
                    out.println("Skipping to song");
                }
                case "status" -> {
                    ForkJoinTask<PlaybackStatus> status =
                            PlayerManager.getPlayers().getStatus();
                    out.println(status.join());
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
            for (String param : cmd.getArgList())
            {
                String finalParam = param.toLowerCase();
                getSongs().getSongs().forEach(song ->
                {
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
                        matchMap.put(song, matches);
                    }
                });
            }
            if (matchMap.size() > 0)
            {
                List<Song> songs = matchMap.entrySet().stream().sorted(Map.Entry
                                .comparingByValue()).filter(entry -> (entry
                                .getValue() / (float) cmd.getArgList().size()) >= 0.65F)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                Collections.reverse(songs);
                out.println("Playing " + songs);
                if (!cmd.hasOption("search"))
                {
                    Queue.getInstance().addAll(songs);
                }
                else
                {
                    for (Song song : songs)
                    {
                        out.println(song);
                    }
                }
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace(out);
        }
    }

    /**
     * Prints command line arguments to the error console.
     */
    public static void printHelp()
    {
        printHelp(System.err);
    }

    /**
     * Prints command line arguments to the provided print stream.
     *
     * @param stream - The stream to print the options to.
     */
    public static void printHelp(PrintStream stream)
    {
        final HelpFormatter formatter = new HelpFormatter();
        PrintWriter writer = new PrintWriter(stream);
        formatter.printHelp(formatter.getWidth(), "utility-name", "CLI Arguments", setupCLIArgs(), null);
        writer.flush();
    }
}
