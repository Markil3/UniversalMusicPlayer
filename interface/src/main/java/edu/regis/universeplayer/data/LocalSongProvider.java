/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import edu.regis.universeplayer.player.Interface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LocalSongProvider implements SongProvider<LocalSong>
{
    private static final Logger logger = LoggerFactory.getLogger(LocalSongProvider.class);
    private static final HashSet<String> formats = new HashSet<>();
    private static final HashSet<String> codecs = new HashSet<>();
    
    private final File source;
    private Connection db;
    
    private final HashSet<LocalSong> songs = new HashSet<>();
    private final HashMap<String, Album> albums = new HashMap<>();
    /**
     * A cache of all song artists.
     */
    private final HashSet<String> artists = new HashSet<>();
    /**
     * A cache of all album genres.
     */
    private final HashSet<String> genres = new HashSet<>();
    /**
     * A cache of all album artists.
     */
    private final HashSet<String> albumArtists = new HashSet<>();
    /**
     * A cache of all album release years.
     */
    private final HashSet<Integer> years = new HashSet<>();
    
    private int updatedSongs;
    private int totalUpdate;
    private final LinkedList<UpdateListener> listeners = new LinkedList<>();
    
    /**
     * Obtains all formats supported by FFMPEG. Note that this list includes
     * video and image formats as well.
     *
     * @return A string array of all supported file formats.
     */
    public static Set<String> getFormats()
    {
        final Pattern FILEPAT = Pattern.compile("^\\s*[D ][E ]\\s*([a-z1-9_]{2,}(,[a-z_]{2,})*)\\s*[A-Za-z1-9 \\(\\)-/'\\.\":]+$");
        final Pattern FILEPAT2 = Pattern.compile("[a-z1-9_]{2,}(,[a-z1-9_]{2,})*");
        Matcher matcher;
        String ffmpegData;
        String name;
        
        synchronized (formats)
        {
            if (formats.isEmpty())
            {
                try
                {
                    Process process = Runtime.getRuntime().exec(new String[] {"ffmpeg", "-formats"});
                    logger.debug("Getting formats");
                    logger.debug("Process complete");
                    try (Scanner scanner = new Scanner(process.getInputStream()))
                    {
                        while (scanner.hasNextLine())
                        {
                            ffmpegData = scanner.nextLine();
                            matcher = FILEPAT.matcher(ffmpegData);
                            if (matcher.matches())
                            {
                                matcher = FILEPAT2.matcher(ffmpegData);
                                if (!matcher.find())
                                {
                                    continue;
                                }
                                name = matcher.group();
                                formats.addAll(Arrays.asList(name.split(",")));
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    logger.error("Could not launch ffmpeg", e);
                }
                logger.debug("Supported formats: {}", formats);
            }
        }
        return formats;
    }
    
    /**
     * Obtains all audio formats supported by FFMPEG.
     *
     * @return A string array of all supported file codecs.
     */
    public static Set<String> getCodecs()
    {
        final Pattern FILEPAT = Pattern.compile("^\\s*D[E.]A[I.][L.][S.]\\s*([a-z1-9_]{2,})\\s*[A-Za-z1-9 \\(\\)-/'\\.\":]+$");
        final Pattern FILEPAT2 = Pattern.compile("[a-z1-9_]{2,}");
        Matcher matcher;
        String ffmpegData;
        String name;
        
        synchronized (codecs)
        {
            if (codecs.isEmpty())
            {
                try
                {
                    logger.debug("Getting codecs");
                    Process process = Runtime.getRuntime().exec(new String[] {"ffmpeg", "-codecs"});
                    logger.debug("Process complete");
                    try (Scanner scanner = new Scanner(process.getInputStream()))
                    {
                        while (scanner.hasNextLine())
                        {
                            ffmpegData = scanner.nextLine();
                            matcher = FILEPAT.matcher(ffmpegData);
                            if (matcher.matches())
                            {
                                matcher = FILEPAT2.matcher(ffmpegData);
                                if (!matcher.find())
                                {
                                    continue;
                                }
                                name = matcher.group();
                                codecs.add(name);
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    logger.error("Could not launch ffmpeg", e);
                }
                logger.debug("Supported codecs: {}", codecs);
            }
        }
        return codecs;
    }
    
    private class SongScanner extends RecursiveAction
    {
        private static final ForkJoinPool service = new ForkJoinPool();
        private static String currentFolder;
        
        private final File file;
        
        SongScanner(File folder)
        {
            this.file = folder;
        }
        
        @Override
        public void compute()
        {
            Process process;
            String line;
            String[] streamData;
            
            String type;
            String codec;
            
            String genre = null;
            String title = null;
            String artist = null;
            String albumTitle = null;
            String albumArtist = null;
            long duration = 0;
            Integer[] track = null;
            Integer[] disc = null;
            
            LocalSong song;
            Album album;
            
            try
            {
                if (file.isDirectory())
                {
                    totalUpdate--;
                    List<SongScanner> tasks = Arrays.stream(Objects.requireNonNullElse(file.listFiles(), new File[0]))
                            .map(SongScanner::new).collect(Collectors.toList());
                    totalUpdate += tasks.size();
                    triggerUpdateListeners();
                    invokeAll(tasks);
                }
                else if (file.getName().lastIndexOf(".") < file.getName().length() - 1)
                {
                    type = file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase();
                    if (getFormats().contains(type))
                    {
                        try
                        {
                            currentFolder = file.getPath();
                            codec = null;
                            process = Runtime.getRuntime().exec(new String[] {"ffprobe", "-hide_banner", file.getAbsolutePath()});
                            process.waitFor();
                            try (Scanner scanner = new Scanner(process.getErrorStream()))
                            {
                                int i = 0;
                                while (scanner.hasNextLine())
                                {
                                    line = scanner.nextLine().trim();
                                    switch (line.toLowerCase().substring(0, line.indexOf(' ') > 0 ? line.indexOf(' ') : line.length()))
                                    {
                                    case "genre" -> {
                                        /*
                                         * We only take the first one, as to
                                         * avoid mishaps with labels after the
                                         * metadata.
                                         */
                                        if (genre == null)
                                        {
                                            genre = line.substring(line.indexOf(':') + 2);
                                        }
                                    }
                                    case "title" -> {
                                        if (title == null)
                                        {
                                            title = line.substring(line.indexOf(':') + 2);
                                        }
                                    }
                                    case "artist" -> {
                                        if (artist == null)
                                        {
                                            artist = line.substring(line.indexOf(':') + 2);
                                        }
                                    }
                                    case "album" -> {
                                        if (albumTitle == null)
                                        {
                                            albumTitle = line.substring(line.indexOf(':') + 2);
                                        }
                                    }
                                    case "album_artist" -> {
                                        if (albumArtist == null)
                                        {
                                            albumArtist = line.substring(line.indexOf(':') + 2);
                                        }
                                    }
                                    case "track" -> {
                                        line = line.substring(line.indexOf(':') + 2);
                                        if (line.indexOf('/') >= 0)
                                        {
                                            track = Arrays.stream(line.split("/")).map(Integer::parseInt).toArray(Integer[]::new);
                                        }
                                        else
                                        {
                                            if (track != null)
                                            {
                                                track[0] = Integer.parseInt(line);
                                            }
                                            else
                                            {
                                                track = new Integer[] {Integer.parseInt(line), -1};
                                            }
                                        }
                                    }
                                    case "tracktotal" -> {
                                        if (track != null)
                                        {
                                            track[1] = Integer.parseInt(line.substring(line.indexOf(':') + 2));
                                        }
                                        else
                                        {
                                            track = new Integer[] {-1, Integer.parseInt(line.substring(line.indexOf(':') + 2))};
                                        }
                                    }
                                    case "disc" -> {
                                        line = line.substring(line.indexOf(':') + 2);
                                        if (line.indexOf('/') >= 0)
                                        {
                                            disc = Arrays.stream(line.split("/")).map(Integer::parseInt).toArray(Integer[]::new);
                                        }
                                        else
                                        {
                                            if (disc != null)
                                            {
                                                disc[0] = Integer.parseInt(line);
                                            }
                                            else
                                            {
                                                disc = new Integer[] {Integer.parseInt(line), -1};
                                            }
                                        }
                                    }
                                    case "disctotal" -> {
                                        if (disc != null)
                                        {
                                            disc[1] = Integer.parseInt(line.substring(line.indexOf(':') + 2));
                                        }
                                        else
                                        {
                                            disc = new Integer[] {-1, Integer.parseInt(line.substring(line.indexOf(':') + 2))};
                                        }
                                    }
                                    case "duration:" -> {
                                        if (duration == 0)
                                        {
                                            line = line.substring(line.indexOf(':') + 2, line.indexOf(','));
                                            duration = Long.parseLong(line.substring(0, 2)) * 3600 * 1000 + Long.parseLong(line.substring(3, 5)) * 60 * 1000 + Long.parseLong(line.substring(6, 8)) * 1000 + (long) (Float.parseFloat(line.substring(8, line.length() - 1)) * 1000);
                                        }
                                    }
                                    case "stream" -> {
                                        streamData = line.split(" ");
                                        if (streamData[2].equals("Audio:"))
                                        {
                                            codec = streamData[3];
                                            if (codec.endsWith(","))
                                            {
                                                codec = codec.substring(0, codec.length() - 1);
                                            }
                                            /*
                                             * If this isn't a supported codec,
                                             * discard.
                                             */
                                            if (!getCodecs().contains(codec))
                                            {
                                                logger.trace("Invalid codec {} for song {}", codec, file);
                                                codec = null;
                                            }
                                            else
                                            {
                                                logger.trace("Found codec {} for song {}", codec, file);
                                            }
                                        }
                                        else
                                        {
                                            logger.trace("Found non-audio stream {} for {}", line, file);
                                        }
                                    }
                                    }
                                }
//                            logger.trace("Finished scanning {}", file);
                            }
                            if (codec != null)
                            {
                                /*
                                 * Update album information.
                                 */
                                synchronized (albums)
                                {
                                    album = albums.get(albumTitle);
                                    if (album == null)
                                    {
                                        album = new Album();
                                        album.name = albumTitle;
                                        albums.put(albumTitle, album);
                                    }
                                }
                                if (album.artists == null && albumArtist != null)
                                {
                                    album.artists = Arrays.stream(albumArtist.split(";")).map(String::trim).toArray(String[]::new);
                                    synchronized (albumArtists)
                                    {
                                        albumArtists.addAll(Arrays.asList(album.artists));
                                    }
                                }
                                if (album.genres == null && genre != null)
                                {
                                    album.genres = Arrays.stream(genre.split(";")).map(String::trim).toArray(String[]::new);
                                    synchronized (genres)
                                    {
                                        genres.addAll(Arrays.asList(album.genres));
                                    }
                                }
                                if (album.totalTracks == 0 && track != null && track[1] > 0)
                                {
                                    album.totalTracks = track[1];
                                }
                                if (album.totalDiscs == 0 && disc != null && disc[1] > 0)
                                {
                                    album.totalDiscs = disc[1];
                                }
                                
                                /*
                                 * Create the song
                                 */
                                song = new LocalSong();
                                song.file = file.getAbsoluteFile();
                                song.type = type;
                                song.codec = codec;
                                song.album = album;
                                if (title != null)
                                {
                                    song.title = title;
                                }
                                if (artist != null)
                                {
                                    song.artists = Arrays.stream(artist.split(";")).map(String::trim).toArray(String[]::new);
                                    synchronized (artists)
                                    {
                                        artists.addAll(Arrays.asList(song.artists));
                                    }
                                }
                                if (disc != null && disc[0] > 0)
                                {
                                    song.disc = disc[0];
                                }
                                if (track != null && track[0] > 0)
                                {
                                    song.trackNum = track[0];
                                }
                                if (duration > 0)
                                {
                                    song.duration = duration;
                                }
                                
                                logger.debug("Caching song {} ({})", song, song.file);
                                
                                updatedSongs++;
                                triggerUpdateListeners();
                                synchronized (songs)
                                {
                                    songs.add(song);
                                }
                            }
                            else
                            {
                                /*
                                 * Never mind, this isn't an updatable song.
                                 */
                                totalUpdate--;
                                triggerUpdateListeners();
                                logger.trace("Could not find codec for {}", file);
                            }
                        }
                        catch (IOException | InterruptedException e)
                        {
                            logger.error("Could not get ffprobe information on " + file, e);
                        }
                    }
                    else
                    {
                        /*
                         * Never mind, this isn't an updatable song.
                         */
                        totalUpdate--;
                        triggerUpdateListeners();
//                    logger.trace("Scanned {}, not applicable", file);
                    }
                }
                else
                {
                    /*
                     * Never mind, this isn't an updatable song.
                     */
                    totalUpdate--;
                    triggerUpdateListeners();
//                logger.trace("Scanned {}, not applicable", file);
                }
            }
            catch (Throwable e)
            {
                logger.error("Error in obtaining song " + this.file, e);
            }
        }
    }
    
    private Connection getDb()
    {
        SQLWarning warning;
        try
        {
            if (this.db == null || this.db.isClosed())
            {
                Class.forName("org.sqlite.JDBC");
                this.db = DriverManager.getConnection("jdbc:sqlite:" + new File(Interface.getDataDir().getAbsolutePath(), "universalmusic.db").getAbsolutePath());
                this.db.setAutoCommit(false);
            }
            warning = this.db.getWarnings();
            while (warning != null)
            {
                logger.warn("SQL Warning: ", warning);
                warning = warning.getNextWarning();
            }
        }
        catch (SQLException | ClassNotFoundException e)
        {
            logger.error("Could not store caching DIR.");
        }
        return this.db;
    }
    
    public LocalSongProvider(File source)
    {
        Connection dbL = null;
        this.source = source;
        if (this.source == null || !this.source.isDirectory())
        {
            throw new IllegalArgumentException("File source must be existing directory");
        }
        
        this.getDb();
        SongScanner.service.submit(() -> {
            Statement state;
            ResultSet result;
            Album album;
            LocalSong song;
            
            try
            {
                logger.debug("Querying database.");
                state = this.getDb().createStatement();
                result = state.executeQuery("SELECT * FROM LOCAL_ALBUMS;");
                while (result.next())
                {
                    album = new Album();
                    album.name = result.getString("album");
                    album.artists = Optional.ofNullable(result.getString("artists")).map(s -> s.split(";")).orElse(new String[0]);
                    album.year = result.getInt("year");
                    album.genres = Optional.ofNullable(result.getString("genres")).map(s -> s.split(";")).orElse(new String[0]);
                    album.totalTracks = result.getInt("tracks");
                    album.totalDiscs = result.getInt("discs");
                    albums.put(album.name, album);
                }
                result = state.executeQuery("SELECT * FROM LOCAL_SONGS;");
                while (result.next())
                {
                    song = new LocalSong();
                    song.file = new File(result.getString("file"));
                    song.codec = result.getString("codec");
                    song.type = result.getString("type");
                    song.title = result.getString("title");
                    song.artists = Optional.ofNullable(result.getString("artists")).map(s -> s.split(";")).orElse(new String[0]);
                    song.trackNum = result.getInt("track");
                    song.disc = result.getInt("disc");
                    song.duration = result.getLong("duration");
                    song.album = Optional.ofNullable(result.getString("album")).map(albums::get).orElse(null);
                    songs.add(song);
                }
                result.close();
                state.close();
            }
            catch (SQLException e)
            {
                logger.error("Could not query SQL database.", e);
            }
            if (songs.isEmpty())
            {
                logger.debug("No songs within database. Scanning...");
                totalUpdate = 1;
                updateSongs();
            }
            else
            {
                try
                {
                    this.getDb().commit();
                    this.getDb().close();
                    updatedSongs = 0;
                    totalUpdate = 0;
                    triggerUpdateListeners();
                }
                catch (SQLException e)
                {
                    logger.error("Could not close database.", e);
                }
            }
        });
    }
    
    /**
     * Scans the local file system for song files.
     */
    private void updateSongs()
    {
        SongScanner.service.submit(new SongScanner(source));
        
        /*
         * Resets the song scanner when ready.
         */
        SongScanner.service.submit(() -> {
            Statement stat;
            ResultSet tableResult;
            while (true)
            {
                if (SongScanner.service.awaitQuiescence(60, TimeUnit.SECONDS))
                {
                    break;
                }
            }
            SongScanner.currentFolder = "";
            updatedSongs = 0;
            totalUpdate = 0;
            triggerUpdateListeners();
            
            /*
             * Update the SQL database.
             */
            try
            {
                logger.debug("Updating local song database.");
                stat = this.getDb().createStatement();
                tableResult = stat.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='LOCAL_SONGS';");
                logger.debug("Table result: {}", tableResult.getCursorName());
                if (!tableResult.next())
                {
                    logger.debug("Creating song table.");
                    /*
                     * Create the table
                     */
                    stat.executeUpdate("CREATE TABLE LOCAL_SONGS" +
                            "(FILE TEXT PRIMARY KEY NOT NULL," +
                            "CODEC CHAR(5)," +
                            "TYPE CHAR(5)," +
                            "TITLE TEXT," +
                            "ARTISTS TEXT," +
                            "TRACK INTEGER," +
                            "DISC INTEGER," +
                            "DURATION BIGINT," +
                            "ALBUM TEXT);");
                }
                else
                {
                    logger.debug("Clearing song table.");
                    stat.executeUpdate("DELETE FROM LOCAL_SONGS;");
                }
                tableResult = stat.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='LOCAL_ALBUMS';");
                if (!tableResult.next())
                {
                    logger.debug("Creating album table.");
                    /*
                     * Create the table
                     */
                    stat.executeUpdate("CREATE TABLE LOCAL_ALBUMS" +
                            "(ALBUM TEXT PRIMARY KEY NOT NULL," +
                            "ARTISTS TEXT," +
                            "YEAR INTEGER," +
                            "GENRES TEXT," +
                            "TRACKS INTEGER," +
                            "DISCS INTEGER);");
                }
                else
                {
                    logger.debug("Clearing album table.");
                    stat.executeUpdate("DELETE FROM LOCAL_ALBUMS;");
                }
                
                tableResult.close();
                
                for (Album album : this.albums.values())
                {
                    stat.executeUpdate("INSERT INTO LOCAL_ALBUMS (ALBUM,ARTISTS,YEAR,GENRES,TRACKS,DISCS) " +
                            "VALUES ('" + album.name + "', '" + String.join(";", album.artists) + "', " + album.year + ", '" + String.join(";", album.genres) + "', " + album.totalTracks + ", " + album.totalDiscs + ");");
                }
                for (LocalSong song : this.songs)
                {
                    stat.executeUpdate("INSERT INTO LOCAL_SONGS (FILE,CODEC,TYPE,TITLE,ARTISTS,TRACK,DISC,DURATION,ALBUM) " +
                            "VALUES ('" + song.file.getAbsolutePath() + "', '" + song.codec + "', '" + song.type + "', '" + song.title + "', '" + String.join(";", song.artists) + "', " + song.trackNum + ", " + song.disc + ", " + song.duration + ", '" + song.album.name + "');");
                }
                stat.close();
                this.getDb().commit();
                this.getDb().close();
                logger.debug("Cache updated.");
            }
            catch (SQLException e)
            {
                logger.error("Error in saving SQLite database", e);
            }
        });
    }
    
    /**
     * Obtains all albums within the collection.
     *
     * @return A list of albums.
     */
    @Override
    public Collection<Album> getAlbums()
    {
        return this.albums.values();
    }
    
    /**
     * Obtains all songs within the collection.
     *
     * @return A list of songs.
     */
    @Override
    public Collection<LocalSong> getSongs()
    {
        synchronized (this.songs)
        {
            return Collections.unmodifiableCollection(this.songs);
        }
    }
    
    /**
     * Obtains a list of all artists.
     *
     * @return All artists.
     */
    @Override
    public Collection<String> getArtists()
    {
        return this.artists;
    }
    
    /**
     * Obtains a list of all album artists.
     *
     * @return All album artists.
     */
    @Override
    public Collection<String> getAlbumArtists()
    {
        return this.albumArtists;
    }
    
    /**
     * Obtains a list of all genres.
     *
     * @return All genres.
     */
    @Override
    public Collection<String> getGenres()
    {
        return this.genres;
    }
    
    /**
     * Obtains a list of all years that have albums.
     *
     * @return All years.
     */
    @Override
    public Collection<Integer> getYears()
    {
        return this.years;
    }
    
    /**
     * Obtains all songs from an album.
     *
     * @param album - The album to obtain
     * @return All songs from the requested album, or null if that album is not in the database.
     */
    @Override
    public Collection<LocalSong> getSongsFromAlbum(Album album)
    {
        synchronized (this.songs)
        {
            return this.songs.stream().filter(song -> song.album.equals(album)).collect(Collectors.toUnmodifiableSet());
        }
    }
    
    /**
     * Obtains all songs written by a given artist.
     *
     * @param artist - The artist to search for
     * @return A list of all songs from the specified artist, or null if that artist is not in the
     * database.
     */
    @Override
    public Collection<LocalSong> getSongsFromArtist(String artist)
    {
        synchronized (this.songs)
        {
            return this.songs.stream().filter(song -> Arrays.asList(song.artists).contains(artist)).collect(Collectors.toUnmodifiableSet());
        }
    }
    
    /**
     * Obtains an album by a specific name.
     *
     * @param name - The name to search for.
     * @return - The first album that matches the given name, or null if that album name is not in
     * the database.
     */
    @Override
    public Album getAlbumByName(String name)
    {
        synchronized (this.albums)
        {
            return this.albums.get(name);
        }
    }
    
    /**
     * Obtains all albums that were written by a certain artist.
     *
     * @param artist - The artist to search for.
     * @return - The collection on matching albums.
     */
    @Override
    public Collection<Album> getAlbumsFromArtist(String artist)
    {
        synchronized (this.albums)
        {
            return this.albums.values().stream().filter(album -> Arrays.asList(album.artists).contains(artist)).collect(Collectors.toUnmodifiableSet());
        }
    }
    
    /**
     * Obtains all albums that match a certain genre
     *
     * @param genre - The genre to search for.
     * @return - The collection on matching albums.
     */
    @Override
    public Collection<Album> getAlbumsFromGenre(String genre)
    {
        synchronized (this.albums)
        {
            return this.albums.values().stream().filter(album -> Arrays.asList(album.genres).contains(genre)).collect(Collectors.toUnmodifiableSet());
        }
    }
    
    /**
     * Obtains all albums that were released a certain year.
     *
     * @param year - The year to search for.
     * @return - The collection on matching albums.
     */
    @Override
    public Collection<Album> getAlbumsFromYear(int year)
    {
        synchronized (this.albums)
        {
            return this.albums.values().stream().filter(album -> album.year == year).collect(Collectors.toUnmodifiableSet());
        }
    }
    
    @Override
    public int getUpdateProgress()
    {
        return this.updatedSongs;
    }
    
    @Override
    public int getTotalUpdateSongs()
    {
        return this.totalUpdate;
    }
    
    @Override
    public String getUpdateText()
    {
        return SongScanner.currentFolder;
    }
    
    @Override
    public void addUpdateListener(UpdateListener listener)
    {
        this.listeners.add(listener);
    }
    
    @Override
    public void removeUpdateListener(UpdateListener listener)
    {
        this.listeners.remove(listener);
    }
    
    protected void triggerUpdateListeners()
    {
        this.listeners.forEach(listener -> listener.onUpdate(this.getUpdateProgress(), this.getTotalUpdateSongs(), this.getUpdateText()));
    }
}
