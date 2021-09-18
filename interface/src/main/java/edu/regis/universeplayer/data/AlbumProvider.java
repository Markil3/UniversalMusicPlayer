package edu.regis.universeplayer.data;

import java.util.Collection;
import java.util.concurrent.Future;

/**
 * Manages all albums that are part of the collection.
 */
public interface AlbumProvider extends DataProvider<Album>
{
    /**
     * Obtains all albums within the collection.
     *
     * @return A list of albums.
     */
    Collection<Album> getAlbums();

    /**
     * Obtains a list of all album artists.
     *
     * @return All album artists.
     */
    Collection<String> getAlbumArtists();

    /**
     * Obtains a list of all genres.
     *
     * @return All genres.
     */
    Collection<String> getGenres();

    /**
     * Obtains a list of all years that have albums.
     *
     * @return All years.
     */
    Collection<Integer> getYears();

    /**
     * Obtains an album by a specific name.
     *
     * @param name - The name to search for.
     * @return - The first album that matches the given name, or null if that album name is not in
     * the database.
     */
    Album getAlbumByName(String name);

    /**
     * Obtains all albums that were written by a certain artist.
     *
     * @param artist - The artist to search for.
     * @return - The collection on matching albums.
     */
    Collection<Album> getAlbumsFromArtist(String artist);

    /**
     * Obtains all albums that match a certain genre
     *
     * @param genre - The genre to search for.
     * @return - The collection on matching albums.
     */
    Collection<Album> getAlbumsFromGenre(String genre);

    /**
     * Obtains all albums that were released a certain year.
     *
     * @param year - The year to search for.
     * @return - The collection on matching albums.
     */
    Collection<Album> getAlbumsFromYear(int year);

    /**
     * Writes an album to the collection.
     * @param album - The album to add.
     */
    Future<Album> writeItem(Album album);
}
