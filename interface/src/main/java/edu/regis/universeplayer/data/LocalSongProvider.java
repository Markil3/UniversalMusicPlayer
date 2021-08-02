/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LocalSongProvider implements SongProvider<LocalSong>
{
    private static final Logger logger = LoggerFactory.getLogger(LocalSongProvider.class);
    private static final HashSet<String> formats = new HashSet<>();
    private static final HashSet<String> codecs = new HashSet<>();
    
    private final File source;
    
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
    
    private class SongScanner implements Runnable
    {
        private static final int serviceThreads = Math.max(Runtime.getRuntime().availableProcessors() - 2, 1);
        private static final ExecutorService service = Executors.newFixedThreadPool(serviceThreads);
        private static String currentFolder;
    
        private final File file;
        
        SongScanner(File folder)
        {
            this.file = folder;
        }
        
        @Override
        public void run()
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
                    for (File subFile : Objects.requireNonNull(file.listFiles()))
                    {
                        totalUpdate++;
                        triggerUpdateListeners();
                        service.submit(new SongScanner(subFile));
                    }
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
    
    public LocalSongProvider(File source)
    {
        this.source = source;
        if (this.source == null || !this.source.isDirectory())
        {
            throw new IllegalArgumentException("File source must be existing directory");
        }
        
        // TODO - Add some sort of caching system
        totalUpdate = 1;
        SongScanner.service.submit(new SongScanner(source));
        
        /*
         * Resets the song scanner when ready.
         */
        SongScanner.service.submit(() -> {
            while (true)
            {
                try
                {
                    if (!SongScanner.service.awaitTermination(60, TimeUnit.SECONDS)) break;
                }
                catch (InterruptedException e)
                {
                    logger.error("Error in waiting for song scan.", e);
                }
            }
            SongScanner.currentFolder = "";
            updatedSongs = 0;
            totalUpdate = 0;
            triggerUpdateListeners();
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
