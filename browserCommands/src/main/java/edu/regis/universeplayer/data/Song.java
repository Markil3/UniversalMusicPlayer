/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Contains data for a song.
 */
public class Song implements Comparable<Song>, Serializable
{
    public String title;
    public String[] artists;
    public int trackNum;
    public int disc;
    public long duration;

    /**
     * A reference to the album this song is part of.
     */
    public Album album;

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
