/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import java.util.*;

/**
 * A song queue contains a list of songs that are to play, along with a way to
 * order them
 *
 * @author William Hubbard
 * @version 0.1
 */
public class Queue extends ArrayList<Song>
{
    private static Queue INSTANCE;
    
    public static Queue getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new Queue();
        }
        return INSTANCE;
    }
    
    private final ArrayList<Song> queueOrder = new ArrayList<>();
    private int currentIndex;
    private boolean shuffle;
    private boolean repeat;
    private final Random random = new Random();
    
    private final LinkedList<SongChangeListener> songListeners = new LinkedList<>();
    private final LinkedList<QueueChangeListener> queueListeners = new LinkedList<>();
    
    public Queue()
    {
    
    }
    
    /**
     * Obtains the song scheduled to play.
     *
     * @return The currently playing song.
     */
    public Song getCurrentSong()
    {
        if (this.currentIndex < 0 || this.currentIndex >= this.queueOrder.size())
        {
            return null;
        }
        return this.queueOrder.get(currentIndex);
    }
    
    /**
     * Obtains the song index scheduled to play.
     *
     * @return The currently playing song index.
     */
    public int getCurrentIndex()
    {
        return this.indexOf(this.getCurrentSong());
    }
    
    /**
     * Checks whether this queue will loop after the end of the song.
     *
     * @return True if the queue loops, false otherwise.
     */
    public boolean isRepeating()
    {
        return this.repeat;
    }
    
    /**
     * Sets whether this queue should loop after the last song plays.
     *
     * @param repeat - Whether the queue loops.
     */
    public void setRepeat(boolean repeat)
    {
        this.repeat = repeat;
    }
    
    /**
     * Checks whether this queue is in shuffle mode.
     *
     * @return True if the queue shuffles, false otherwise.
     */
    public boolean isShuffling()
    {
        return this.shuffle;
    }
    
    /**
     * Sets whether this queue should be shuffled or not.
     *
     * @param shuffle - Whether or not the queue shuffles.
     */
    public void setShuffle(boolean shuffle)
    {
        this.shuffle = shuffle;
        this.queueOrder.clear();
    }
    
    /**
     * Skips to the next song.
     *
     * @return The new song, or null if we finished the list.
     */
    public Song skipNext()
    {
        int index = this.currentIndex;
        if (++this.currentIndex >= this.queueOrder.size())
        {
            if (this.repeat)
            {
                this.currentIndex = 0;
                /*
                 * Reshuffle the queue as needed.
                 */
                this.queueOrder.clear();
                this.getOrder();
                if (this.currentIndex != index)
                {
                    this.triggerSongChangeListeners();
                }
                return this.getCurrentSong();
            }
            else
            {
                /*
                 * Make sure we aren't infinitely increasing the current index.
                 */
                this.currentIndex = this.queueOrder.size() - 1;
                if (this.currentIndex != index)
                {
                    this.triggerSongChangeListeners();
                }
                return null;
            }
        }
        if (this.currentIndex != index)
        {
            this.triggerSongChangeListeners();
        }
        return this.getCurrentSong();
    }
    
    /**
     * Skips to the previous song.
     *
     * @return The previous song, or the first one if we go underboard.
     */
    public Song skipPrev()
    {
        int index = this.currentIndex;
        if (--this.currentIndex < 0)
        {
            if (this.repeat)
            {
                this.currentIndex = this.queueOrder.size() - 1;
            }
            else
            {
                this.currentIndex = 0;
            }
        }
        if (this.currentIndex != index)
        {
            this.triggerSongChangeListeners();
        }
        return this.getCurrentSong();
    }
    
    /**
     * Skips to the song at the provided song index.
     *
     * @param index - The new index. This is not relational to the queue.
     * @return The new song, or null if we finished the list.
     */
    public Song skipToSong(int index)
    {
        Song song = this.get(index);
        index = this.queueOrder.indexOf(song);
        if (this.currentIndex != index)
        {
            this.currentIndex = index;
            this.triggerSongChangeListeners();
        }
        return this.getCurrentSong();
    }
    
    private ArrayList<Song> getOrder()
    {
        LinkedList<Song> indexesRemaining;
        if (this.queueOrder.isEmpty())
        {
            this.queueOrder.addAll(this);
            if (this.shuffle)
            {
                indexesRemaining = new LinkedList<>(this.queueOrder);
                this.queueOrder.clear();
                while (indexesRemaining.size() > 0)
                {
                    this.queueOrder.add(indexesRemaining.remove(random.nextInt(indexesRemaining.size())));
                }
            }
        }
        return this.queueOrder;
    }
    
    @Override
    public boolean add(Song song)
    {
        Song oldSong = this.getCurrentSong();
        int newIndex;
        if (super.add(song))
        {
            if (this.shuffle)
            {
                newIndex = random.nextInt(this.queueOrder.size());
                if (newIndex <= this.currentIndex)
                {
                    this.currentIndex++;
                }
                this.queueOrder.add(newIndex, song);
            }
            else
            {
                this.queueOrder.add(song);
            }
            this.triggerQueueChangeListeners();
            if (this.getCurrentSong() != oldSong)
            {
                this.triggerSongChangeListeners();
            }
            return true;
        }
        return false;
    }
    
    @Override
    public void add(int index, Song song)
    {
        Song oldSong = this.getCurrentSong();
        int newIndex;
        super.add(index, song);
        if (this.shuffle)
        {
            newIndex = this.random.nextInt(this.queueOrder.size());
        }
        else
        {
            newIndex = index;
        }
        if (newIndex <= this.currentIndex)
        {
            this.currentIndex++;
        }
        this.queueOrder.add(newIndex, song);
        if (this.getCurrentSong() != oldSong)
        {
            this.triggerSongChangeListeners();
        }
        this.triggerQueueChangeListeners();
    }
    
    @Override
    public Song remove(int index)
    {
        Song removed = super.remove(index);
        boolean current = this.getCurrentSong() == removed;
        if (removed != null)
        {
            index = this.queueOrder.indexOf(removed);
            if (index <= this.currentIndex)
            {
                this.currentIndex--;
            }
            this.queueOrder.remove(removed);
            this.triggerQueueChangeListeners();
            if (current)
            {
                this.triggerSongChangeListeners();
            }
        }
        return removed;
    }
    
    @Override
    public boolean remove(Object o)
    {
        int index = this.indexOf(o);
        boolean removed = this.getCurrentSong() == o;
        if (super.remove(o))
        {
            if (index <= this.currentIndex)
            {
                this.currentIndex--;
            }
            this.queueOrder.remove(o);
            this.triggerQueueChangeListeners();
            if (removed)
            {
                this.triggerSongChangeListeners();
            }
            return true;
        }
        return false;
    }
    
    @Override
    public void clear()
    {
        super.clear();
        this.queueOrder.clear();
        this.currentIndex = 0;
        this.triggerQueueChangeListeners();
        this.triggerSongChangeListeners();
    }
    
    @Override
    public boolean addAll(Collection<? extends Song> c)
    {
        int newIndex;
        int length = this.size();
        Song oldSong = this.getCurrentSong();
        if (super.addAll(c))
        {
            for (Song song : c)
            {
                if (this.shuffle)
                {
                    newIndex = random.nextInt(this.queueOrder.size());
                    if (newIndex <= this.currentIndex)
                    {
                        this.currentIndex++;
                    }
                    this.queueOrder.add(newIndex, song);
                }
                else
                {
                    this.queueOrder.add(song);
                }
            }
            this.triggerQueueChangeListeners();
            if (this.getCurrentSong() != oldSong)
            {
                this.triggerSongChangeListeners();
            }
            return true;
        }
        return false;
    }
    
    @Override
    public boolean addAll(int index, Collection<? extends Song> c)
    {
        int newIndex;
        int i = 0;
        Song oldSong = this.getCurrentSong();
        if (super.addAll(index, c))
        {
            for (Song song : c)
            {
                if (this.shuffle)
                {
                    newIndex = random.nextInt(this.queueOrder.size());
                }
                else
                {
                    newIndex = index + i++;
                }
                if (newIndex <= this.currentIndex)
                {
                    this.currentIndex++;
                }
                this.queueOrder.add(newIndex, song);
            }
            this.triggerQueueChangeListeners();
            if (this.getCurrentSong() != oldSong)
            {
                this.triggerSongChangeListeners();
            }
            return true;
        }
        return false;
    }
    
    @Override
    protected void removeRange(int fromIndex, int toIndex)
    {
        Song oldSong = this.getCurrentSong();
        for (int i = fromIndex; i < toIndex; i++)
        {
            this.queueOrder.remove(this.get(i));
            if (i <= this.currentIndex)
            {
                this.currentIndex--;
            }
        }
        super.removeRange(fromIndex, toIndex);
        this.triggerQueueChangeListeners();
        if (this.getCurrentSong() != oldSong)
        {
            this.triggerSongChangeListeners();
        }
    }
    
    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean success;
        Song oldSong = this.getCurrentSong();
        for (int i = 0, removed = 0; i <= this.currentIndex && i < this.size(); i++)
        {
            if (c.contains(this.get(i)))
            {
                this.currentIndex--;
            }
        }
        this.queueOrder.removeAll(c);
        success = super.removeAll(c);
        this.triggerQueueChangeListeners();
        if (this.getCurrentSong() != oldSong)
        {
            this.triggerSongChangeListeners();
        }
        return success;
    }
    
    @Override
    public boolean retainAll(Collection<?> c)
    {
        boolean success;
        Song oldSong = this.getCurrentSong();
        for (int i = 0, removed = 0; i <= this.currentIndex && i < this.size(); i++)
        {
            if (!c.contains(this.get(i)))
            {
                this.currentIndex--;
            }
        }
        this.queueOrder.retainAll(c);
        success = super.retainAll(c);
        this.triggerQueueChangeListeners();
        if (this.getCurrentSong() != oldSong)
        {
            this.triggerSongChangeListeners();
        }
        return success;
    }
    
    /**
     * Adds a listener for when the current song changes.
     *
     * @param listener - The listener to add.
     */
    public void addSongChangeListener(SongChangeListener listener)
    {
        this.songListeners.add(listener);
    }
    
    /**
     * Removes a listener for when the current song changes.
     *
     * @param listener - The listener to remove.
     */
    public void removeSongChangeListener(SongChangeListener listener)
    {
        this.songListeners.remove(listener);
    }
    
    /**
     * Tells all listeners that the song changed.
     */
    protected void triggerSongChangeListeners()
    {
        this.songListeners.forEach(listener -> listener.onSongChange(this));
    }
    
    /**
     * Adds a listener for when the queue contents changes.
     *
     * @param listener - The listener to add.
     */
    public void addQueueChangeListener(QueueChangeListener listener)
    {
        this.queueListeners.add(listener);
    }
    
    /**
     * Removes a listener for when the queue contents changes.
     *
     * @param listener - The listener to remove.
     */
    public void removeQueueChangeListener(QueueChangeListener listener)
    {
        this.queueListeners.remove(listener);
    }
    
    /**
     * Tells all listeners that the queue changed.
     */
    protected void triggerQueueChangeListeners()
    {
        this.queueListeners.forEach(listener -> listener.onQueueChange(this));
    }
    
    public interface SongChangeListener extends EventListener
    {
        /**
         * Called when the song of a queue changes.
         *
         * @param queue - The queue whose song changed.
         */
        void onSongChange(Queue queue);
    }
    
    public interface QueueChangeListener extends EventListener
    {
        /**
         * Called when the queue contents change.
         *
         * @param queue - The queue that changed.
         */
        void onQueueChange(Queue queue);
    }
}
