/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Contains data for a song.
 */
public class Song implements Comparable<Song>, Serializable
{
    /**
     * The internal ID representing this song in the database.
     */
    public int id;
    /**
     * The name of the song.
     */
    public String title;
    /**
     * Artists who contributed to the song.
     */
    public String[] artists;
    /**
     * Which track number in the album the song belongs to.
     */
    public int trackNum;
    /**
     * Which disc
     */
    public int disc;
    /**
     * How long the song is in milliseconds.
     */
    public long duration;

    /**
     * A reference to the album this song is part of.
     */
    public Album album;

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof Song song))
        {
            return false;
        }
        return trackNum == song.trackNum && disc == song.disc && title.equals(song.title) && Arrays.equals(artists, song.artists) && album.equals(song.album);
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hash(title, trackNum, disc, album);
        result = 31 * result + Arrays.hashCode(artists);
        return result;
    }

    @Override
    public int compareTo(Song o)
    {
        if (o != null)
        {
            int comp = this.album != null ?
                    (o.album != null ? this.album.compareTo(o.album) :
                            -1) : o.album != null ? 1 : 0;
            if (comp == 0)
            {
                comp = Integer.compare(this.disc, o.disc);
                if (comp == 0)
                {
                    comp = Integer.compare(this.trackNum, o.trackNum);
                    if (comp == 0)
                    {
                        comp = this.title != null ?
                                (o.title != null ?
                                        this.title.compareTo(o.title) :
                                        -1) : o.title != null ? 1 : 0;
                    }
                }
            }
            return comp;
        }
        else
        {
            return -1;
        }
    }

    @Override
    public String toString()
    {
        return "Song{" +
                "title='" + title + '\'' +
                ", artists=" + Arrays.toString(artists) +
                ", album=" + album +
                '}';
    }
}
