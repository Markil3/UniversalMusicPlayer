class Album
{
    name;
    artists;
    year;
    genres;
    totalTracks;
    totalDiscs;
}

class Song
{
    /**
     * The name of the song.
     */
    title;
    /**
     * Artists who contributed to the song.
     */
    artists;
    /**
     * Which track number in the album the song belongs to.
     */
    trackNum;
    /**
     * Which disc
     */
    disc;
    /**
     * How long the song is in milliseconds.
     */
    duration;

    /**
     * A reference to the album this song is part of.
     */
    album;
}

class InternetSong extends Song
{
    /**
     * The location of the song.
     */
    location;
}