package edu.regis.universeplayer.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinTask;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DefaultAlbumProvider extends DatabaseProvider<Album> implements AlbumProvider
{
    private static final Logger logger =
            LoggerFactory.getLogger(DefaultAlbumProvider.class);

    /**
     * Obtains all albums within the collection.
     *
     * @return A list of albums.
     */
    @Override
    public Collection<Album> getAlbums()
    {
        return this.getCollection();
    }

    /**
     * Obtains a list of all album artists.
     *
     * @return All album artists.
     */
    @Override
    public Collection<String> getAlbumArtists()
    {
        return this.getCollection().stream().filter(a -> a.artists != null)
                   .map(a -> new HashSet<>(Arrays.asList(a.artists)))
                   .reduce(new HashSet<>(), (strings, strings2) -> {
                       HashSet<String> comb = new HashSet<>();
                       comb.addAll(strings);
                       comb.addAll(strings2);
                       return comb;
                   });
    }

    /**
     * Obtains a list of all genres.
     *
     * @return All genres.
     */
    @Override
    public Collection<String> getGenres()
    {
        return this.getCollection().stream().filter(a -> a.genres != null)
                   .map(a -> new HashSet<>(Arrays.asList(a.genres)))
                   .reduce(new HashSet<>(), (strings, strings2) -> {
                       HashSet<String> comb = new HashSet<>();
                       comb.addAll(strings);
                       comb.addAll(strings2);
                       return comb;
                   });
    }

    /**
     * Obtains a list of all years that have albums.
     *
     * @return All years.
     */
    @Override
    public Collection<Integer> getYears()
    {
        return this.getCollection().stream().map(a -> a.year)
                   .collect(Collectors.toSet());
    }

    /**
     * Obtains an album by a specific name.
     *
     * @param name - The name to search for.
     * @return - The first album that matches the given name, or null if that
     * album name is not in the database.
     */
    @Override
    public Album getAlbumByName(String name)
    {
        return this.getCollection().stream()
                   .filter(a -> a.name == null && name == null ||
                           a.name != null && a.name.equals(name))
                   .findFirst().orElse(null);
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
        return this.getCollection().stream().filter(a -> {
            if (a.artists != null)
            {
                for (int i = 0; i < a.artists.length; i++)
                {
                    if (a.artists[i].equals(artist))
                    {
                        return true;
                    }
                }
            }
            return false;
        }).collect(Collectors.toSet());
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
        return this.getCollection().stream().filter(a -> {
            if (a.genres != null)
            {
                for (int i = 0; i < a.genres.length; i++)
                {
                    if (a.genres[i].equals(genre))
                    {
                        return true;
                    }
                }
            }
            return false;
        }).collect(Collectors.toSet());
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
        return this.getCollection().stream().filter(a -> a.year == year)
                   .collect(Collectors.toSet());
    }

    /**
     * Obtains the name of
     *
     * @return The name of the database table. This is case insensitive.
     */
    @Override
    protected String getDatabaseTable()
    {
        return "albums";
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
        return "CREATE TABLE albums (album TEXT PRIMARY KEY," +
                "artists TEXT," +
                "year INTEGER," +
                "genres TEXT," +
                "tracks INTEGER," +
                "discs INTEGER);";
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
    protected Album readResult(ResultSet result) throws SQLException
    {
        Album album = new Album();
        album.id = result.getRow();
        album.name = result.getString("album");
        album.artists =
                Optional.ofNullable(result.getString("artists"))
                        .map(s -> s.split(";")).stream()
                        .mapMulti((BiConsumer<String[], Consumer<String>>) (strings, objectConsumer) -> {
                            for (String string : strings)
                            {
                                if (!string.isEmpty())
                                {
                                    objectConsumer.accept(string);
                                }
                            }
                        }).map(String::trim).toArray(String[]::new);
        album.year = result.getInt("year");
        album.genres =
                Optional.ofNullable(result.getString("genres"))
                        .map(s -> s.split(";")).stream()
                        .mapMulti((BiConsumer<String[], Consumer<String>>) (strings, objectConsumer) -> {
                            for (String string : strings)
                            {
                                if (!string.isEmpty())
                                {
                                    objectConsumer.accept(string);
                                }
                            }
                        }).map(String::trim).toArray(String[]::new);
        album.totalTracks = result.getInt("tracks");
        album.totalDiscs = result.getInt("discs");
        return album;
    }

    /**
     * Called to obtain the properties of an object to write.
     *
     * @param item - The item to serialize.
     * @return Properties to write.
     */
    @Override
    protected Map<String, Object> serializeItem(Album item)
    {
        LinkedHashMap<String, Object> returnValue = new LinkedHashMap<>();
        returnValue.put("album", item.name);
        returnValue.put("artists", Arrays.stream(item.artists).reduce("",
                (s1, s2) -> s1.isEmpty() ? s2 : s1 + ";" + s2));
        returnValue.put("year", item.year);
        returnValue.put("genres", Arrays.stream(item.genres).reduce("",
                (s1, s2) -> s1.isEmpty() ? s2 : s1 + ";" + s2));
        returnValue.put("tracks", item.totalTracks);
        returnValue.put("discs", item.totalDiscs);
        return returnValue;
    }

    /**
     * Converts a piece of data into a string that will display in the update
     * text.
     *
     * @param data - The data to stringify.
     * @return A string representation of the data being updated.
     */
    @Override
    protected String stringifyResult(Album data)
    {
        return data.name;
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
}
