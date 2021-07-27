/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.File;
import java.io.IOException;

/**
 * This will specifically import an MP3 file.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class MP3Song extends LocalSong
{
    public MP3Song(File file)
    {
        Mp3File metadata;
        ID3v1 tag1;
        ID3v2 tag2;
        this.file = file;
        try
        {
            metadata = new Mp3File(file);
            this.album = new Album();
            if (metadata.hasId3v1Tag())
            {
                tag1 = metadata.getId3v1Tag();
                this.title = tag1.getTitle();
                this.artists = tag1.getArtist().split(";");
                /**
                 * Trim the artists as needed.
                 */
                for (int i = 0, l = this.artists.length; i < l; i++)
                {
                    this.artists[i] = this.artists[i].trim();
                }
                this.trackNum = Integer.parseInt(tag1.getTrack().split("/")[0]);

                this.album.name = tag1.getAlbum();
                this.album.genres = tag1.getGenreDescription().split(";");
                for (int i = 0, l = this.album.genres.length; i < l; i++)
                {
                    this.album.genres[i] = this.album.genres[i].trim();
                }
                this.album.year = Integer.parseInt(tag1.getYear());
            }
            /**
             * Make sure that the v2 tag doesn't contain any contradictory information.
             */
            if (metadata.hasId3v2Tag())
            {
                tag2 = metadata.getId3v2Tag();
                if (!tag2.getTitle().isEmpty())
                {
                    this.title = tag2.getTitle();
                }
                if (!tag2.getTitle().isEmpty())
                {
                    this.artists = tag2.getArtist().split(";");
                    /**
                     * Trim the artists as needed.
                     */
                    for (int i = 0, l = this.artists.length; i < l; i++)
                    {
                        this.artists[i] = this.artists[i].trim();
                    }
                }
                if (!tag2.getTitle().isEmpty())
                {
                    this.trackNum = Integer.parseInt(tag2.getTrack().split("/")[0]);
                }

                if (!tag2.getAlbum().isEmpty())
                {
                    this.album.name = tag2.getAlbum();
                }
                if (!tag2.getAlbumArtist().isEmpty())
                {
                    this.album.artists = tag2.getAlbumArtist().split(";");
                    /**
                     * Trim the artists as needed.
                     */
                    for (int i = 0, l = this.album.artists.length; i < l; i++)
                    {
                        this.album.artists[i] = this.album.artists[i].trim();
                    }
                }
                if (!tag2.getGenreDescription().isEmpty())
                {
                    this.album.genres = tag2.getGenreDescription().split(";");
                    for (int i = 0, l = this.album.genres.length; i < l; i++)
                    {
                        this.album.genres[i] = this.album.genres[i].trim();
                    }
                }
                if (!tag2.getYear().isEmpty())
                {
                    this.album.year = Integer.parseInt(tag2.getYear());
                }
            }
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("Invalid MP3 File", e);
        }
        catch (UnsupportedTagException | InvalidDataException e)
        {
        }

    }
}
