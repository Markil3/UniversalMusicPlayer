/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import edu.regis.universeplayer.browser.Browser;
import edu.regis.universeplayer.browserCommands.QuerySongData;

public class InternetSongProvider extends DatabaseProvider<InternetSong> implements SongProvider<InternetSong>
{
    private static final Logger logger = LoggerFactory.getLogger(InternetSongProvider.class);

    private static InternetSongProvider INSTANCE;

    private final AtomicInteger progress = new AtomicInteger(0);
    private final AtomicInteger updating = new AtomicInteger(0);
    private String updateItem;

    private final AlbumProvider albums;

    public static InternetSongProvider getInstance()
    {
        return INSTANCE;
    }

    private final LinkedList<UpdateListener> listeners = new LinkedList<>();
    
    public InternetSongProvider(AlbumProvider albums)
    {
        this.albums = albums;
        INSTANCE = this;
    }

    @Override
    public int getUpdateProgress()
    {
        int sup = super.getUpdateProgress();
        if (sup == 0)
        {
            sup = this.progress.get();
        }
        return sup;
    }

    @Override
    public int getTotalUpdates()
    {
        int sup = super.getTotalUpdates();
        if (sup == 0)
        {
            sup = this.updating.get();
        }
        return sup;
    }

    @Override
    public String getUpdateText()
    {
        String sup = super.getUpdateText();
        if (sup == null || sup.isEmpty())
        {
            sup = this.updateItem;
        }
        return sup;
    }

    /**
     * Obtains the name of
     *
     * @return The name of the database table. This is case insensitive.
     */
    @Override
    protected String getDatabaseTable()
    {
        return "internet_songs";
    }

    /**
     * Obtains the SQL string used to create the database table should it be
     * necessary.
     *
     * @return The SQL command that creates the database table. It should take
     * the format of "CREATE TABLE name (param1 type, param2 type);"
     */
    @Override
    protected String createDatabaseTable()
    {
        /*
         * Create the table
         */
        return "CREATE TABLE internet_songs" +
                "(url TEXT PRIMARY KEY NOT NULL," +
                "title TEXT," +
                "artists TEXT," +
                "track INTEGER," +
                "disc INTEGER," +
                "duration BIGINT," +
                "album TEXT);";
    }

    @Override
    public AlbumProvider getAlbumProvider()
    {
        return this.albums;
    }

    /**
     * Called when an entry is read from the database and is ready to be
     * parsed.
     * <p>
     * Note that this method is called for every row. Do NOT call {@link
     * ResultSet#next()}!
     *
     * @param result The result that is read from.
     */
    @Override
    protected InternetSong readResult(ResultSet result) throws SQLException
    {
        InternetSong song = new InternetSong();
        song.location = result.getURL("url");
        song.title = result.getString("title");
        song.artists =
                Arrays.stream(result.getString("artists").split(";"))
                      .map(String::trim).toArray(String[]::new);
        song.trackNum = result.getInt("track");
        song.disc = result.getInt("disc");
        song.duration = result.getLong("duration");
        try
        {
            getAlbumProvider().joinUpdate();
            song.album = getAlbumProvider().getAlbumByName(result.getString(
                    "album"));
        }
        catch (InterruptedException e)
        {
            logger.error("Couldn't wait for album provider for song {}", song
                    , e);
        }
        return song;
    }

    /**
     * Called to obtain the properties of an object to write.
     *
     * @param item - The item to serialize.
     * @return Properties to write.
     */
    @Override
    protected Map<String, Object> serializeItem(InternetSong item)
    {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("url", item.location);
        map.put("title", item.title);
        map.put("artists", Arrays.stream(item.artists).reduce("",
                (s1, s2) -> s1 + ";" + s2));
        map.put("track", item.trackNum);
        map.put("disc", item.disc);
        map.put("duration", item.duration);
        map.put("album",
                Optional.ofNullable(item.album).map(a -> a.name).orElse(null));
        return map;
    }

    /**
     * Converts a piece of data into a string that will display in the update
     * text.
     *
     * @param data - The data to stringify.
     * @return A string representation of the data being updated.
     */
    @Override
    protected String stringifyResult(InternetSong data)
    {
        return Optional.ofNullable(data.album).map(a -> a.name).orElse(null) +
                "/" + data.title;
    }

    /**
     * A callback for when the database scan is complete.
     * <p>
     * Note that this method is called from the same thread that the scanner is
     * from.
     * </p>
     *
     * @return A fork-join task to invoke. This may be null.
     */
    @Override
    protected ForkJoinTask[] onComplete()
    {
        return null;
    }

    public ForkJoinTask<InternetSong> addSong(URL url)
    {
        AddInternetTask task = new AddInternetTask(url);
        this.service.execute(task);
        return task;
    }

    private class AddInternetTask extends ForkJoinTask<InternetSong>
    {
        private final URL loc;
        private InternetSong item;

        public AddInternetTask(URL song)
        {
            this.loc = song;
        }

        /**
         * Returns the result that would be returned by {@link #join}, even if
         * this task completed abnormally, or {@code null} if this task is not
         * known to have been completed.  This method is designed to aid
         * debugging, as well as to support extensions. Its use in any other
         * context is discouraged.
         *
         * @return the result, or {@code null} if not completed
         */
        @Override
        public InternetSong getRawResult()
        {
            return this.item;
        }

        /**
         * Forces the given value to be returned as a result.  This method is
         * designed to support extensions, and should not in general be called
         * otherwise.
         *
         * @param value the value
         */
        @Override
        protected void setRawResult(InternetSong value)
        {
            this.item = value;
        }

        /**
         * Immediately performs the base action of this task and returns true
         * if, upon return from this method, this task is guaranteed to have
         * completed. This method may return false otherwise, to indicate that
         * this task is not necessarily complete (or is not known to be
         * complete), for example in asynchronous actions that require explicit
         * invocations of completion methods. This method may also throw an
         * (unchecked) exception to indicate abnormal exit. This method is
         * designed to support extensions, and should not in general be called
         * otherwise.
         *
         * @return {@code true} if this task is known to have completed normally
         */
        @Override
        protected boolean exec()
        {
            InternetSong data;
            try
            {
                data =
                        (InternetSong) Browser.getInstance().sendObject(new QuerySongData(this.loc)).get();
            }
            catch (InterruptedException | ExecutionException | IOException e)
            {
                this.completeExceptionally(e);
                return false;
            }
            if (data != null)
            {
                try
                {
                    writeItem(data).get();
                }
                catch (InterruptedException | ExecutionException e)
                {
                    this.completeExceptionally(e);
                    return false;
                }
            }
            this.complete(data);
            return true;
        }
    }
}
