/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universe_player.data;

/**
 * Contains data for a song.
 */
public abstract class Song implements Comparable<Song>
{
    public String title;
    public String[] artists;
    public int trackNum;
    public int disc;
    /**
     * A reference to the album this song is part of.
     */
    public Album album;

    @Override
    public int compareTo(Song o)
    {
        if (o != null)
        {
            int comp = this.album.compareTo(o.album);
            if (comp == 0)
            {
                comp = Integer.compare(this.disc, o.disc);
                if (comp == 0)
                {
                    comp = Integer.compare(this.trackNum, o.trackNum);
                    if (comp == 0)
                    {
                        comp = this.title.compareTo(o.title);
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
}
