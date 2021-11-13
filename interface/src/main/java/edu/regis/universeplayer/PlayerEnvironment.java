package edu.regis.universeplayer;

import edu.regis.universeplayer.data.Queue;
import edu.regis.universeplayer.data.*;
import edu.regis.universeplayer.gui.Interface;
import edu.regis.universeplayer.player.PlayerManager;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
        AtomicBoolean streamRunning = new AtomicBoolean(true);
        Thread inputRunner;
        try
        {
            socket = new Socket("localhost", ConfigManager.PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            Scanner remoteOut = new Scanner(socket.getInputStream());
            logger.debug("Connecting to remote");
            out.writeObject(args);
            /*
             * Start a stream for
             */
            inputRunner = new Thread(() -> {
                Scanner sysIn = new Scanner(System.in);
                String line;
                while (streamRunning.get())
                {
                    if (sysIn.hasNextLine())
                    {
                        line = sysIn.nextLine();
                        try
                        {
                            out.writeInt(5);
                            out.writeObject(line);
                        }
                        catch (IOException e)
                        {
                            logger.error("Could not write console input to remote instance", e);
                        }
                    }
                    else
                    {
                        try
                        {
                            Thread.sleep(100);
                        }
                        catch (InterruptedException e)
                        {
                            logger.error("Error in input runner while sleeping", e);
                        }
                    }
                }
                sysIn.close();
            }, "InputRunner");
            inputRunner.start();
            logger.debug("Waiting for output");
            while (remoteOut.hasNextLine())
            {
                data = remoteOut.nextLine();
                if (data.equals("END"))
                {
                    break;
                }
                System.out.println(data);
            }
            streamRunning.set(false);
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
        logger.info("Starting application {}", cmd);

        /*
         * Create a new thread to handle new instances of the player.
         */
        InstanceConnector connector = new InstanceConnector();
        new Thread(connector, "InstanceConnector").start();

        /*
         * Initialize the song and album collections. Both of these will
         * automatically populate themselves upon construction on separate
         * threads.
         */
        ALBUMS_INSTANCE = new DefaultAlbumProvider();
        SONGS_INSTANCE =
                new CompiledSongProvider(new LocalSongProvider(ALBUMS_INSTANCE),
                        new InternetSongProvider(ALBUMS_INSTANCE));

        /*
         * Initializes the song queue and the various players used.
         */
        Queue queue = Queue.getInstance();
        PlayerManager playback = PlayerManager.getPlayers();
        /*
         * Every time the queue triggers a change, this callback will tell the
         * player manager to play the next one.
         */
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
        /*
         * Whenever the playback manager finishes a song, this callback will
         * tell the queue to skip to the next song. This triggers the above
         * callback.
         */
        playback.addPlaybackListener(status ->
        {
            logger.info("Receiving {} from {}", status.getInfo(), status
                    .getSource());
            if (status.getInfo().getStatus() == PlaybackStatus.FINISHED)
            {
                Queue.getInstance().skipNext();
            }
        });

        /*
         * Show the GUI, if necessary.
         */
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
        runArguments(cmd, System.out, System.in);

        /*
         * Stops the interface connector and shut down players as needed when we
         * close the program.
         */
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
                    .desc("Pauses playback.")
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

            Option addAlbum = Option.builder().longOpt("addAlbum")
                    .hasArg().optionalArg(true).argName("name")
                    .desc("Adds an album to the library")
                    .build();
            OPTIONS.addOption(addAlbum);
            Option addRemote = Option.builder().longOpt("add")
                    .numberOfArgs(6).argName("url title artist1;artist2 track disc albumName")
                    .desc("Adds an internet song to the library.")
                    .build();
            OPTIONS.addOption(addRemote);
            Option name = Option.builder().longOpt("name")
                    .hasArg().argName("title")
                    .desc("Used for searching and adding elements to specify the name of what song you want to find/add.")
                    .build();
            OPTIONS.addOption(name);
            Option artists = Option.builder("a").longOpt("artist")
                    .hasArg().argName("artist1;artist2")
                    .desc("A semicolon-separated list used for searching and adding elements to specify artists to find/add to your element.")
                    .build();
            OPTIONS.addOption(artists);
            Option year = Option.builder("y").longOpt("year")
                    .hasArg().argName("year")
                    .desc("Used for searching and adding elements to specify the year of the album to find/add.")
                    .build();
            OPTIONS.addOption(year);
            Option genres = Option.builder("g").longOpt("genre")
                    .hasArg().argName("genre1;genre2")
                    .desc("A semicolon-separated list used for searching and adding elements to specify genres to find/add to your album.")
                    .build();
            OPTIONS.addOption(genres);
            Option albumName = Option.builder("b").longOpt("album")
                    .hasArg().argName("name")
                    .desc("Used for searching and adding songs to specify the name of the album to find/add to.")
                    .build();
            OPTIONS.addOption(albumName);
            Option track = Option.builder().longOpt("track")
                    .hasArg().argName("trackNum")
                    .desc("Used for searching and adding elements to specify the track number of the song or the number of tracks in the album, depending on the context.")
                    .build();
            OPTIONS.addOption(track);
            Option disc = Option.builder("d").longOpt("disc")
                    .hasArg().argName("discNum")
                    .desc("Used for searching and adding elements to specify the disc the song is on or the number of discs in the album, depending on the context.")
                    .build();
            OPTIONS.addOption(disc);
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
    public static void runArguments(CommandLine cmd, PrintStream out, InputStream in)
    {
        try
        {
//            out.println(cmd.toString());
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
                case "addAlbum" -> {
                    String albumName = op.getValue();
                    String[] artists = null;
                    String[] genres = null;
                    int year = -1;
                    int tracks = -1;
                    int discs = -1;
                    if (albumName == null)
                    {
                        if (cmd.hasOption("name"))
                        {
                            albumName = cmd.getOptionValue("name");
                        }
                        if (albumName == null)
                        {
                            if (cmd.hasOption("album"))
                            {
                                albumName = cmd.getOptionValue("album");
                            }
                        }
                    }
                    if (cmd.hasOption("artist") && cmd.getOptionValue("artist") != null)
                    {
                        artists = cmd.getOptionValue("artist").split("\\s*;\\s*");
                    }
                    if (cmd.hasOption("genre") && cmd.getOptionValue("genre") != null)
                    {
                        genres = cmd.getOptionValue("genre").split("\\s*;\\s*");
                    }
                    if (cmd.hasOption("year") && cmd.getOptionValue("year") != null)
                    {
                        year = Integer.parseInt(cmd.getOptionValue("year"));
                    }
                    if (cmd.hasOption("track") && cmd.getOptionValue("track") != null)
                    {
                        tracks = Integer.parseInt(cmd.getOptionValue("track"));
                    }
                    if (cmd.hasOption("disc") && cmd.getOptionValue("disc") != null)
                    {
                        discs = Integer.parseInt(cmd.getOptionValue("disc"));
                    }

                    /*
                     * Prompt the user for missing information
                     */
                    try (Scanner scanner = new Scanner(in))
                    {
                        if (albumName == null)
                        {
                            out.println("What is the name of the album: ");
                            albumName = scanner.nextLine();
                        }
                        if (artists == null)
                        {
                            out.println("Please enter the album artists as a semicolon-separated list: ");
                            artists = scanner.nextLine().split("\\s*;\\s*");
                        }
                        if (genres == null)
                        {
                            out.println("Please enter the genres as a semicolon-separated list: ");
                            genres = scanner.nextLine().split("\\s*;\\s*");
                        }
                        while (year == -1)
                        {
                            out.println("Please enter the album year: ");
                            try
                            {
                                year = Integer.parseInt(scanner.nextLine());
                            }
                            catch (NumberFormatException e)
                            {
                                out.println("Invalid number entered.");
                            }
                        }
                        while (tracks == -1)
                        {
                            out.println("Please enter the number of tracks on the album: ");
                            try
                            {
                                tracks = Integer.parseInt(scanner.nextLine());
                            }
                            catch (NumberFormatException e)
                            {
                                out.println("Invalid number entered.");
                            }
                        }
                        while (discs == -1)
                        {
                            out.println("Please enter the number of discs on the album: ");
                            try
                            {
                                discs = Integer.parseInt(scanner.nextLine());
                            }
                            catch (NumberFormatException e)
                            {
                                out.println("Invalid number entered.");
                            }
                        }
                    }

                    Album album = new Album();
                    album.name = albumName;
                    album.artists = artists;
                    album.genres = genres;
                    album.year = year;
                    album.totalTracks = tracks;
                    album.totalDiscs = discs;
                    out.println(album.toString());
                }
                case "add" -> {

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
                if (!cmd.hasOption("search"))
                {
                    if (songs.size() > 0)
                    {
                        out.print("Playing ");
                    }
                }
                if (songs.size() > 0)
                {
                    out.print(songs.get(0));
                    if (songs.size() > 1)
                    {
                        Song song;
                        ListIterator<Song> iter = songs.listIterator(1);
                        while (iter.hasNext())
                        {
                            song = iter.next();
                            if (!cmd.hasOption("search"))
                            {
                                out.print("; ");
                            }
                            else
                            {
                                out.println();
                            }
                            out.print(song.toString());
                        }
                    }
                }
                out.println();
                if (!cmd.hasOption("search"))
                {
                    Queue.getInstance().addAll(songs);
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
