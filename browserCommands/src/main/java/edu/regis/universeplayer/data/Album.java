/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import javax.swing.ImageIcon;

public class Album implements Comparable<Album>
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
    public int compareTo(Album o)
    {
        if (o != null)
        {
            return this.name.compareTo(o.name);
        }
        else
        {
            return -1;
        }
    }
}
