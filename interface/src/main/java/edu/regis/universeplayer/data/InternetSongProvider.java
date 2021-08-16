/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import edu.regis.universeplayer.browser.InternetSong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class InternetSongProvider implements SongProvider<InternetSong>
{
    private static final Logger logger = LoggerFactory.getLogger(InternetSongProvider.class);
    private static final ExecutorService service = Executors.newSingleThreadExecutor();
    private static InternetSongProvider INSTANCE;
    
    public static InternetSongProvider getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new InternetSongProvider();
        }
        return INSTANCE;
    }
    
    private final HashMap<URL, InternetSong> songs = new HashMap<>();
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
    
    private InternetSongProvider()
    {
        this.getSongCache();
    }
    
    private void getSongCache()
    {
        service.submit(new SongQuery());
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
    public Collection<InternetSong> getSongs()
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
    public Collection<InternetSong> getSongsFromAlbum(Album album)
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
    public Collection<InternetSong> getSongsFromArtist(String artist)
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
        return "";
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
    
    public Future<InternetSong> addSong(URL url, String title, String albumName, String artists, String genres)
    {
        return service.submit(() -> {
            Album album = albums.get(albumName);
            Statement state = DatabaseManager.getDb().createStatement();
            if (album == null)
            {
                album = new Album();
                album.name = albumName;
                albums.put(albumName, album);
                synchronized (DatabaseManager.getDb())
                {
                    state.executeUpdate("INSERT INTO internet_albums (album) VALUES ('" + albumName + "');");
                }
            }
            album.artists = Arrays.stream(artists.split(";")).map(String::trim).toArray(String[]::new);
            album.genres = Arrays.stream(genres.split(";")).map(String::trim).toArray(String[]::new);
            synchronized (DatabaseManager.getDb())
            {
                state.executeUpdate("UPDATE internet_albums SET artists='" + String.join(";", album.artists) + "' WHERE album='" + albumName + "';");
                state.executeUpdate("UPDATE internet_albums SET genres='" + String.join(";", album.genres) + "' WHERE album='" + albumName + "';");
            }
            InternetSong song = new InternetSong();
            song.location = url;
            song.title = title;
            song.album = album;
            song.artists = album.artists.clone();
            
            // TODO - Evaluate the song
            
            songs.put(url, song);
            
            StringBuilder sql = new StringBuilder("INSERT INTO internet_songs ");
            StringBuilder columns = new StringBuilder("(");
            StringBuilder values = new StringBuilder("(");
            
            columns.append("url,");
            values.append('\'').append(url.toString().replaceAll("'", "''")).append("',");
            if (title != null && !title.isEmpty())
            {
                columns.append("title,");
                values.append('\'').append(title.replaceAll("'", "''")).append("',");
            }
            if (song.artists != null)
            {
                columns.append("artists,");
                values.append('\'').append(String.join(";", song.artists)).append("',");
            }
            columns.append("album");
            values.append('\'').append(albumName).append("'");
            
            columns.append(") VALUES ");
            values.append(");");
            sql.append(columns);
            sql.append(values);
            synchronized (DatabaseManager.getDb())
            {
                state.executeUpdate(sql.toString());
            }
            this.triggerUpdateListeners();
            return song;
        });
    }
    
    private class SongQuery implements Runnable
    {
        SongQuery()
        {
        }
        
        @Override
        public void run()
        {
            Statement state;
            ResultSet result;
            Album album;
            InternetSong song;
            int numAlbums = 0, numSongs = 0;
            
            try
            {
                logger.debug("Querying database.");
                /*
                 * Check if the table exists
                 */
                synchronized (DatabaseManager.getDb())
                {
                    /*
                     * Make sure that a "null" album is available
                     */
                    
                    if (albums.get(null) == null)
                    {
                        album = new Album();
                        album.name = "Unknown";
                        albums.put(null, album);
                    }
                    
                    if (albums.get("Unknown") == null)
                    {
                        albums.put("Unknown", albums.get(null));
                    }
                    
                    state = DatabaseManager.getDb().createStatement();
                    result = state.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='INTERNET_ALBUMS';");
                    if (!result.next())
                    {
                        logger.debug("Creating album table.");
                        /*
                         * Create the table
                         */
                        state.executeUpdate("CREATE TABLE INTERNET_ALBUMS" +
                                "(ALBUM TEXT PRIMARY KEY NOT NULL," +
                                "ARTISTS TEXT," +
                                "YEAR INTEGER," +
                                "GENRES TEXT," +
                                "TRACKS INTEGER," +
                                "DISCS INTEGER);");
                    }
                    else
                    {
                        result = state.executeQuery("SELECT * FROM INTERNET_ALBUMS;");
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
                            numAlbums++;
                        }
                    }
                    result = state.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='INTERNET_SONGS';");
                    if (!result.next())
                    {
                        logger.debug("Creating song table.");
                        /*
                         * Create the table
                         */
                        state.executeUpdate("CREATE TABLE INTERNET_SONGS" +
                                "(URL TEXT PRIMARY KEY NOT NULL," +
                                "TITLE TEXT," +
                                "ARTISTS TEXT," +
                                "TRACK INTEGER," +
                                "DISC INTEGER," +
                                "DURATION BIGINT," +
                                "ALBUM TEXT);");
                    }
                    else
                    {
                        result = state.executeQuery("SELECT * FROM INTERNET_SONGS;");
                        while (result.next())
                        {
                            if (result.getString("url") == null)
                            {
                                continue;
                            }
                            URL url;
                            try
                            {
                                url = new URL(result.getString("url"));
                            }
                            catch (MalformedURLException e)
                            {
                                logger.error("Could not parse URL " + result.getString("url"), e);
                                continue;
                            }
                            song = songs.get(url);
                            if (song == null)
                            {
                                song = new InternetSong();
                                song.location = url;
                                songs.put(song.location, song);
                            }
                            song.title = result.getString("title");
                            song.artists = Optional.ofNullable(result.getString("artists")).map(s -> s.split(";")).orElse(new String[0]);
                            song.trackNum = result.getInt("track");
                            song.disc = result.getInt("disc");
                            song.duration = result.getLong("duration");
                            song.album = Optional.ofNullable(result.getString("album")).map(albums::get).orElse(albums.get("Unknown"));
                            numSongs++;
                        }
                    }
                    state.close();
                }
            }
            catch (SQLException e)
            {
                logger.error("Could not query SQL database.", e);
            }
            logger.debug("Query complete, retrieved {} albums and {} songs", numAlbums, numSongs);
            updatedSongs = 0;
            totalUpdate = 0;
            triggerUpdateListeners();
        }
    }
}
