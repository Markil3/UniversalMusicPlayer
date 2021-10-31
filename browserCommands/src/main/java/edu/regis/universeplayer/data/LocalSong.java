/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

/**
 * This song represents a song found on the local file system.
 */
public class LocalSong extends Song
{
    /**
     * The file the song is stored at.
     */
    public File file;
    /**
     * The format type the song is stored in.
     */
    public String type;
    /**
     * The encoding format the song is recorded in.
     */
    public String codec;
    /**
     * The last modification time of this song file, as returned by {@link
     * File#lastModified()}.
     */
    public long lastMod;

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof LocalSong localSong))
        {
            return false;
        }
        if (!super.equals(o))
        {
            return false;
        }
        return file.equals(localSong.file);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), file);
    }

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
}
