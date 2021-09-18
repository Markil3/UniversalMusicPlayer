package edu.regis.universeplayer.data;

import java.util.Set;

/**
 * A data provider serves as a place to get songs, albums, or any other
 * collection of information needed by the music player.
 */
public interface DataProvider<T>
{
    /**
     * Checks to see if we are updating any songs.
     *
     * @return True if we are updating the song cache.
     */
    default boolean isUpdating()
    {
        return this.getTotalUpdates() != 0;
    }

    /**
     * If there is an update in progress, this halts the calling thread until
     * the update is complete.
     */
    void joinUpdate() throws InterruptedException;

    /**
     * When we are updating the cache, this method obtains the number of items
     * already updated.
     *
     * @return The number of items successfully updated.
     */
    int getUpdateProgress();

    /**
     * Determines whether or not we are updating the cache and, if so, gets the
     * total number of items we need to update
     *
     * @return The total number of items to update. A 0 means that we do not
     * have any items to update, and a negative number means that we are
     * currently calculating how many items we need to update.
     */
    int getTotalUpdates();

    /**
     * Determines the text displayed for the progress of updates
     *
     * @return The update status text.
     */
    String getUpdateText();


    /**
     * Obtains the collection of items.
     *
     * @return A collection of items parsed from the database.
     */
    Set<T> getCollection();

    /**
     * Adds a listener for song updates.
     *
     * @param listener - The listener to add.
     */
    void addUpdateListener(UpdateListener listener);

    /**
     * Removes a listener for song updates.
     *
     * @param listener - The listener to remove.
     */
    void removeUpdateListener(UpdateListener listener);
}
