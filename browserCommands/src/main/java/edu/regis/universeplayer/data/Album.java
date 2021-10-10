/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import javax.swing.ImageIcon;

public class Album implements Comparable<Album>, Serializable
{
    public int id;
    public String name;
    public String[] artists;
    public transient ImageIcon art;
    public int year;
    public String[] genres;
    public int totalTracks;
    public int totalDiscs;

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof Album album))
        {
            return false;
        }
        return year == album.year && totalTracks == album.totalTracks && totalDiscs == album.totalDiscs && name.equals(album.name) && Arrays.equals(artists, album.artists) && Arrays.equals(genres, album.genres);
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hash(name, year, totalTracks, totalDiscs);
        result = 31 * result + Arrays.hashCode(artists);
        result = 31 * result + Arrays.hashCode(genres);
        return result;
    }

    @Override
    public int compareTo(Album o)
    {
        if (o != null && o.name != null)
        {
            if (this.name == null)
            {
                return 1;
            }
            else
            {
                return this.name.compareToIgnoreCase(o.name);
            }
        }
        else
        {
            if (this.name == null)
            {
                return 0;
            }
            else
            {
                return -1;
            }
        }
    }

    @Override
    public String toString()
    {
        return "Album{" +
                "name='" + name + '\'' +
                ", artists=" + Arrays.toString(artists) +
                '}';
    }
}
