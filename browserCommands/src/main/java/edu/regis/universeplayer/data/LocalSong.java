/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import java.io.File;
import java.util.Arrays;

/**
 * This song represents a song found on the local file system.
 */
public class LocalSong extends Song
{
    public File file;
    public String type;
    public String codec;

    @Override
    public int compareTo(Song o)
    {
        int compare = super.compareTo(o);
        if (compare == 0)
        {
            if (o instanceof LocalSong)
            {
                compare = this.file.compareTo(((LocalSong) o).file);
            }
        }
        return compare;
    }

    @Override
    public String toString()
    {
        return "Song{" +
                "title='" + title + '\'' +
                ", artists=" + Arrays.toString(artists) +
                ", album=" + album +
                ", url=" + file.getAbsolutePath() +
                '}';
    }
}
