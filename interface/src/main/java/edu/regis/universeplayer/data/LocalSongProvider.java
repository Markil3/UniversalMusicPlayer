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
    
    private final HashMap<File, LocalSong> songs = new HashMap<>();
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
    
    private Connection getDb()
    {
        SQLWarning warning;
        try
        {
            if (this.db == null || this.db.isClosed())
            {
                Class.forName("org.sqlite.JDBC");
                this.db = DriverManager.getConnection("jdbc:sqlite:" + new File(Interface.getDataDir().getAbsolutePath(), "universalmusic.db").getAbsolutePath());
            }
            warning = this.db.getWarnings();
            while (warning != null)
            {
                logger.warn("SQL Warning: ", warning);
                warning = warning.getNextWarning();
            }
            this.db.clearWarnings();
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
        getSongCache();
    }
    
    private void getSongCache()
    {
        SongScanner.service.submit(new SongQuery(true));
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
            return Collections.unmodifiableCollection(this.songs.values());
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
            return this.songs.values().stream().filter(song -> song.album.equals(album)).collect(Collectors.toUnmodifiableSet());
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
            return this.songs.values().stream().filter(song -> Arrays.asList(song.artists).contains(artist)).collect(Collectors.toUnmodifiableSet());
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
            
            Statement state = null;
            ResultSet result;
            
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
                            state = getDb().createStatement();
                            result = state.executeQuery("SELECT mod FROM local_songs WHERE file='" + this.file.getAbsolutePath() + "';");
                            if (result.next())
                            {
                                if (result.getLong(1) >= this.file.lastModified())
                                {
                                    /*
                                     * No modifications needed
                                     */
                                    updatedSongs++;
                                    triggerUpdateListeners();
                                    return;
                                }
                                else
                                {
                                    state.executeUpdate("UPDATE local_songs SET mod = " + this.file.lastModified() + " WHERE file='" + this.file.getAbsolutePath() + "';");
                                }
                            }
                            currentFolder = file.getPath();
                            triggerUpdateListeners();
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
                                result = state.executeQuery("SELECT album FROM local_albums WHERE album='" + albumTitle + "';");
                                if (!result.next())
                                {
                                    state.executeUpdate("INSERT INTO local_albums (album) VALUES ('" + albumTitle + "');");
                                }
                                result = state.executeQuery("SELECT artists FROM local_albums WHERE album='" + albumTitle + "';");
                                if (result.getString("artists") == null && albumArtist != null)
                                {
                                    state.executeUpdate("UPDATE local_albums SET artists='" + Arrays.stream(albumArtist.split(";")).map(String::trim).collect(Collectors.joining(";")) + "' WHERE album='" + albumTitle + "';");
                                }
                                // TODO - Can we get year metadata?
                                result = state.executeQuery("SELECT genres FROM local_albums WHERE album='" + albumTitle + "';");
                                if (result.getString("genres") == null && genre != null)
                                {
                                    state.executeUpdate("UPDATE local_albums SET genres='" + Arrays.stream(genre.split(";")).map(String::trim).collect(Collectors.joining(";")) + "' WHERE album='" + albumTitle + "';");
                                }
                                result = state.executeQuery("SELECT tracks FROM local_albums WHERE album='" + albumTitle + "';");
                                if (result.getInt("tracks") == 0 && track != null && track[1] > 0)
                                {
                                    state.executeUpdate("UPDATE local_albums SET tracks=" + track[1] + " WHERE album='" + albumTitle + "';");
                                }
                                result = state.executeQuery("SELECT discs FROM local_albums WHERE album='" + albumTitle + "';");
                                if (result.getInt("discs") == 0 && disc != null && disc[1] > 0)
                                {
                                    state.executeUpdate("UPDATE local_albums SET tracks=" + disc[1] + " WHERE album='" + albumTitle + "';");
                                }
                                
                                /*
                                 * Create the song
                                 */
                                result = state.executeQuery("SELECT title FROM local_songs WHERE file='" + file.getAbsolutePath() + "';");
                                if (result.next())
                                {
                                    logger.debug("Updating song cache for {} ({})", title, file);
                                    StringBuilder sql = new StringBuilder("UPDATE local_songs SET ");
                                    sql.append("codec='").append(codec).append("', ");
                                    sql.append("type='").append(codec).append("', ");
                                    if (title != null && !title.isEmpty())
                                    {
                                        sql.append("title='").append(title).append("', ");
                                    }
                                    else
                                    {
                                        sql.append("title='").append(file.getName()).append("', ");
                                    }
                                    if (artist != null && !artist.isEmpty())
                                    {
                                        sql.append("artists='").append(Optional.of(artist).map(s -> s.split(";")).stream().flatMap(Arrays::stream).map(String::trim).collect(Collectors.joining(";"))).append("', ");
                                    }
                                    else
                                    {
                                        sql.append("artists=NULL, ");
                                    }
                                    if (track != null && track[0] > 0)
                                    {
                                        sql.append("track=").append(track[0]).append(", ");
                                    }
                                    else
                                    {
                                        sql.append("track=NULL, ");
                                    }
                                    if (disc != null && disc[0] > 0)
                                    {
                                        sql.append("disc=").append(disc[0]).append(", ");
                                    }
                                    else
                                    {
                                        sql.append("disc=NULL, ");
                                    }
                                    if (duration != 0)
                                    {
                                        sql.append("duration=").append(duration).append(", ");
                                    }
                                    else
                                    {
                                        sql.append("duration=NULL, ");
                                    }
                                    if (albumTitle != null && !albumTitle.isEmpty())
                                    {
                                        sql.append("album='").append(albumTitle).append("', ");
                                    }
                                    else
                                    {
                                        sql.append("album=NULL, ");
                                    }
                                    sql.append("mod=").append(file.lastModified());
                                    sql.append(" WHERE file='").append(file.getAbsolutePath()).append("';");
                                    state.executeUpdate(sql.toString());
                                }
                                else
                                {
                                    logger.debug("Caching song {} ({})", title, file);
                                    StringBuilder sql = new StringBuilder("INSERT INTO local_songs ");
                                    StringBuilder columns = new StringBuilder("(");
                                    StringBuilder values = new StringBuilder("(");
                                    
                                    columns.append("file,");
                                    values.append('\'').append(file.getAbsolutePath()).append("',");
                                    columns.append("codec,");
                                    values.append('\'').append(codec).append("',");
                                    columns.append("type,");
                                    values.append('\'').append(type).append("',");
                                    if (title != null && !title.isEmpty())
                                    {
                                        columns.append("title,");
                                        values.append('\'').append(title).append("',");
                                    }
                                    if (artist != null && !artist.isEmpty())
                                    {
                                        columns.append("artists,");
                                        values.append('\'').append(Optional.of(artist).map(s -> s.split(";")).stream().flatMap(Arrays::stream).map(String::trim).collect(Collectors.joining(";"))).append("',");
                                    }
                                    if (track != null && track[0] > 0)
                                    {
                                        columns.append("track,");
                                        values.append(track[0]).append(",");
                                    }
                                    if (disc != null && disc[0] > 0)
                                    {
                                        columns.append("disc,");
                                        values.append(disc[0]).append(",");
                                    }
                                    if (duration > 0)
                                    {
                                        columns.append("duration,");
                                        values.append(duration).append(",");
                                    }
                                    if (albumTitle != null && !albumTitle.isEmpty())
                                    {
                                        columns.append("album,");
                                        values.append('\'').append(albumTitle).append("',");
                                    }
                                    
                                    columns.append("mod");
                                    values.append(file.lastModified());
                                    
                                    columns.append(") VALUES ");
                                    values.append(");");
                                    sql.append(columns);
                                    sql.append(values);
                                    state.executeUpdate(sql.toString());
                                }
                                
                                updatedSongs++;
                                triggerUpdateListeners();
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
                        catch (SQLException e)
                        {
                            logger.error("Could not write song " + file + " to database", e);
                        }
                        catch (IOException | InterruptedException e)
                        {
                            logger.error("Could not get ffprobe information on " + file, e);
                        }
                        finally
                        {
                            state.close();
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
    
    private class SongQuery extends RecursiveAction
    {
        private final boolean scan;
        
        SongQuery(boolean scan)
        {
            this.scan = scan;
        }
        
        @Override
        protected void compute()
        {
            Statement state;
            ResultSet result;
            Album album;
            LocalSong song;
            
            try
            {
                logger.debug("Querying database.");
                /*
                 * Check if the table exists
                 */
                state = getDb().createStatement();
                result = state.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='LOCAL_ALBUMS';");
                if (!result.next())
                {
                    logger.debug("Creating album table.");
                    /*
                     * Create the table
                     */
                    state.executeUpdate("CREATE TABLE LOCAL_ALBUMS" +
                            "(ALBUM TEXT PRIMARY KEY NOT NULL," +
                            "ARTISTS TEXT," +
                            "YEAR INTEGER," +
                            "GENRES TEXT," +
                            "TRACKS INTEGER," +
                            "DISCS INTEGER);");
                }
                else
                {
                    result = state.executeQuery("SELECT * FROM LOCAL_ALBUMS;");
                    while (result.next())
                    {
                        album = albums.get(result.getString("album"));
                        if (album == null)
                        {
                            album = new Album();
                            album.name = result.getString("album");
                            albums.put(album.name, album);
                        }
                        album.artists = Optional.ofNullable(result.getString("artists")).map(s -> s.split(";")).orElse(new String[0]);
                        album.year = result.getInt("year");
                        album.genres = Optional.ofNullable(result.getString("genres")).map(s -> s.split(";")).orElse(new String[0]);
                        album.totalTracks = result.getInt("tracks");
                        album.totalDiscs = result.getInt("discs");
                    }
                }
                result = state.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='LOCAL_SONGS';");
                if (!result.next())
                {
                    logger.debug("Creating song table.");
                    /*
                     * Create the table
                     */
                    state.executeUpdate("CREATE TABLE LOCAL_SONGS" +
                            "(FILE TEXT PRIMARY KEY NOT NULL," +
                            "CODEC CHAR(5)," +
                            "TYPE CHAR(5)," +
                            "TITLE TEXT," +
                            "ARTISTS TEXT," +
                            "TRACK INTEGER," +
                            "DISC INTEGER," +
                            "DURATION BIGINT," +
                            "ALBUM TEXT," +
                            "MOD BIGINT);");
                }
                else
                {
                    result = state.executeQuery("SELECT * FROM LOCAL_SONGS;");
                    while (result.next())
                    {
                        song = songs.get(new File(result.getString("file")));
                        if (song == null)
                        {
                            song = new LocalSong();
                            song.file = new File(result.getString("file"));
                            songs.put(song.file, song);
                        }
                        song.codec = result.getString("codec");
                        song.type = result.getString("type");
                        song.title = result.getString("title");
                        song.artists = Optional.ofNullable(result.getString("artists")).map(s -> s.split(";")).orElse(new String[0]);
                        song.trackNum = result.getInt("track");
                        song.disc = result.getInt("disc");
                        song.duration = result.getLong("duration");
                        song.album = Optional.ofNullable(result.getString("album")).map(albums::get).orElse(null);
                    }
                }
                state.close();
            }
            catch (SQLException e)
            {
                logger.error("Could not query SQL database.", e);
            }
            logger.debug("Query complete.");
            updatedSongs = 0;
            totalUpdate = 0;
            triggerUpdateListeners();
            
            if (scan)
            {
                logger.debug("Scanning for changes...");
                totalUpdate = 1;
                invokeAll(new SongScanner(source), new ScanCompletion());
            }
        }
    }
    
    private class ScanCompletion extends RecursiveAction
    {
        @Override
        protected void compute()
        {
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
            invokeAll(new SongQuery(false));
        }
    }
}
