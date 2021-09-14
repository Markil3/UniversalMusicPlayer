/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

public class InternetSong extends Song
{
    private static final Logger logger = LoggerFactory
            .getLogger(InternetSong.class);
    public URL location;

    @Override
    public int compareTo(Song o)
    {
        int compare = super.compareTo(o);
        if (compare == 0)
        {
            if (o instanceof LocalSong)
            {
                try
                {
                    compare =
                            this.location.toURI()
                                         .compareTo(((InternetSong) o).location
                                                 .toURI());
                }
                catch (URISyntaxException e)
                {
                    logger.error("Could not compare locations {} and {}",
                            this.location, ((InternetSong) o).location, e);
                }
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
                ", url=" + location +
                '}';
    }
}
