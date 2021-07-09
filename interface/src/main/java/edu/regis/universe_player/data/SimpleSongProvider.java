/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universe_player.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * A test song database that automatically generates a handful of songs.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class SimpleSongProvider implements SongProvider
{
    private ArrayList<Album> albums;
    private ArrayList<Song> songs;

    @Override
    public Collection<Album> getAlbums()
    {
        if (albums == null)
        {
            albums = new ArrayList<>();
            Random random = new Random();
            StringBuilder builder;
            String albumName, albumArtist;
            Album album;
            for (int albumNum = 0, numAlbums = random
                    .nextInt(5) + 5; albumNum < numAlbums; albumNum++)
            {
                builder = new StringBuilder();
                for (int i = 0, l = random.nextInt(10) + 10; i < l; i++)
                {
                    builder.append((char) (random.nextInt(26) + 97));
                }
                albumName = builder.toString();

                builder = new StringBuilder();
                for (int i = 0, l = random.nextInt(10) + 10; i < l; i++)
                {
                    builder.append((char) (random.nextInt(26) + 97));
                }
                albumArtist = builder.toString();

                album = new Album()
                {
                };

                album.name = albumName;
                album.artists = new String[]{albumArtist};
                album.genres = new String[]{"Soundtrack"};
                album.year = 2019;
                album.totalTracks = random.nextInt(10) + 10;
                album.id = albumNum;

                albums.add(album);
            }
        }

        return albums;
    }

    @Override
    public Collection<Song> getSongs()
    {
        if (songs == null)
        {
            /*
             * Generate a list of songs
             */
            Random random = new Random();
            StringBuilder builder;
            String songTitle;
            Song song;
            int songNum, numSongs;
            songs = new ArrayList<>();
            for (Album album : this.getAlbums())
            {
                for (songNum = 0, numSongs = album.totalTracks; songNum < numSongs; songNum++)
                {
                    builder = new StringBuilder();
                    for (int i = 0, l = random.nextInt(10) + 10; i < l; i++)
                    {
                        builder.append((char) (random.nextInt(26) + 97));
                    }
                    songTitle = builder.toString();

                    song = new Song()
                    {
                    };
                    song.title = songTitle;
                    song.disc = 1;
                    song.trackNum = songNum + 1;
                    song.artists = album.artists.clone();
                    song.album = album;
                    songs.add(song);
                }
                album.totalTracks = numSongs;
            }
        }
        return this.songs;
    }

    @Override
    public Collection<String> getArtists()
    {
        return this.songs.stream().flatMap(song -> Arrays.stream(song.artists)).sorted()
                         .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getGenres()
    {
        return this.songs.stream().flatMap(song -> Arrays.stream(song.album.genres)).sorted()
                         .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getAlbumArtists()
    {
        return this.albums.stream().flatMap(album -> Arrays.stream(album.artists)).sorted()
                          .collect(Collectors.toList());
    }

    @Override
    public Collection<Integer> getYears()
    {
        return this.albums.stream().map(album -> album.year).sorted()
                          .collect(Collectors.toList());
    }

    @Override
    public Collection<Song> getSongsFromAlbum(Album album)
    {
        return this.songs.stream().filter(song -> song.album == album).sorted()
                         .collect(Collectors.toList());
    }

    @Override
    public Collection<Song> getSongsFromArtist(String artist)
    {
        return this.songs.stream().filter(song -> Arrays.asList(song.artists).contains(artist)).sorted()
                         .collect(Collectors.toList());
    }

    @Override
    public Album getAlbumByName(String name)
    {
        return this.albums.stream().filter(album -> album.name.equals(name)).findFirst().orElse(null);
    }

    @Override
    public Collection<Album> getAlbumsFromArtist(String artist)
    {
        return this.albums.stream().filter(album -> Arrays.asList(album.artists).contains(artist))
                          .sorted().collect(Collectors.toList());
    }

    @Override
    public Collection<Album> getAlbumsFromGenre(String genre)
    {
        return this.albums.stream().filter(album -> Arrays.asList(album.genres).contains(genre))
                          .sorted().collect(Collectors.toList());
    }

    @Override
    public Collection<Album> getAlbumsFromYear(int year)
    {
        return this.albums.stream().filter(album -> album.year == year)
                          .sorted().collect(Collectors.toList());
    }
}
