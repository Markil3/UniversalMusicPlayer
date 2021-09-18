/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.regis.universeplayer.ConfigManager;

public class LocalSongProvider extends DatabaseProvider<LocalSong> implements SongProvider<LocalSong>
{
    private static final Logger logger = LoggerFactory
            .getLogger(LocalSongProvider.class);
    private static final ResourceBundle langs = ResourceBundle
            .getBundle("lang.interface", Locale.getDefault());
    private static final HashSet<String> formats = new HashSet<>();
    private static final HashSet<String> codecs = new HashSet<>();

    private final AlbumProvider albums;

    private final AtomicInteger progress = new AtomicInteger(0);
    private final AtomicInteger updating = new AtomicInteger(0);
    private String updateItem;

    /**
     * Obtains all formats supported by FFMPEG. Note that this list includes
     * video and image formats as well.
     *
     * @return A string array of all supported file formats.
     */
    public static Set<String> getFormats()
    {
        final Pattern FILEPAT = Pattern
                .compile("^\\s*[D ][E ]\\s*([a-z1-9_]{2,}(,[a-z_]{2,})*)\\s*[A-Za-z1-9 ()-/'.\":]+$");
        final Pattern FILEPAT2 = Pattern
                .compile("[a-z1-9_]{2,}(,[a-z1-9_]{2,})*");
        Matcher matcher;
        String ffmpegData;
        String name;

        synchronized (formats)
        {
            if (formats.isEmpty())
            {
                try
                {
                    Process process = Runtime.getRuntime()
                                             .exec(new String[]{"ffmpeg", "-formats"});
                    logger.debug("Getting formats");
                    logger.debug("Process complete");
                    try (Scanner scanner = new Scanner(process
                            .getInputStream()))
                    {
                        while (scanner.hasNextLine())
                        {
                            ffmpegData = scanner.nextLine();
                            matcher = FILEPAT.matcher(ffmpegData);
                            if (matcher.matches())
                            {
                                matcher = FILEPAT2.matcher(ffmpegData);
                                if (!matcher.find())
                                {
                                    continue;
                                }
                                name = matcher.group();
                                formats.addAll(Arrays.asList(name.split(",")));
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    logger.error("Could not launch ffmpeg", e);
                }
                logger.debug("Supported formats: {}", formats);
            }
        }
        return formats;
    }

    /**
     * Obtains all audio formats supported by FFMPEG.
     *
     * @return A string array of all supported file codecs.
     */
    public static Set<String> getCodecs()
    {
        final Pattern FILEPAT = Pattern
                .compile("^\\s*D[E.]A[I.][L.][S.]\\s*([a-z1-9_]{2,})\\s*.+$");
        final Pattern FILEPAT2 = Pattern.compile("[a-z1-9_]{2,}");
        Matcher matcher;
        String ffmpegData;
        String name;

        synchronized (codecs)
        {
            if (codecs.isEmpty())
            {
                try
                {
                    logger.debug("Getting codecs");
                    Process process = Runtime.getRuntime()
                                             .exec(new String[]{"ffmpeg", "-codecs"});
                    logger.debug("Process complete");
                    try (Scanner scanner = new Scanner(process
                            .getInputStream()))
                    {
                        while (scanner.hasNextLine())
                        {
                            ffmpegData = scanner.nextLine();
                            matcher = FILEPAT.matcher(ffmpegData);
                            if (matcher.matches())
                            {
                                matcher = FILEPAT2.matcher(ffmpegData);
                                if (!matcher.find())
                                {
                                    continue;
                                }
                                name = matcher.group();
                                codecs.add(name);
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    logger.error("Could not launch ffmpeg", e);
                }
                logger.debug("Supported codecs: {}", codecs);
            }
        }
        return codecs;
    }

    public LocalSongProvider(AlbumProvider albums)
    {
        this.albums = albums;
    }

    @Override
    public int getUpdateProgress()
    {
        int sup = super.getUpdateProgress();
        if (sup == 0)
        {
            sup = this.progress.get();
        }
        return sup;
    }

    @Override
    public int getTotalUpdates()
    {
        int sup = super.getTotalUpdates();
        if (sup == 0)
        {
            sup = this.updating.get();
        }
        return sup;
    }

    @Override
    public String getUpdateText()
    {
        String sup = super.getUpdateText();
        if (sup == null || sup.isEmpty())
        {
            if (this.updateItem != null)
            {
                sup = new Formatter().format(langs.getString("update" +
                        ".local"), this.updateItem).toString();
            }
        }
        return sup;
    }

    @Override
    public AlbumProvider getAlbumProvider()
    {
        return this.albums;
    }

    /**
     * Obtains the name of
     *
     * @return The name of the database table. This is case insensitive.
     */
    @Override
    protected String getDatabaseTable()
    {
        return "local_songs";
    }

    /**
     * Obtains the SQL string used to create the database table should it be
     * necessary.
     *
     * @return The SQL command that creates the database table. It should take
     * the format of "CREATE TABLE name (param1 type, param2 type);"
     */
    @Override
    protected String createDatabaseTable()
    {
        /*
         * Create the table
         */
        return "CREATE TABLE local_songs" +
                "(file TEXT PRIMARY KEY NOT NULL," +
                "codec CHAR(5)," +
                "type CHAR(5)," +
                "title TEXT," +
                "artists TEXT," +
                "track INTEGER," +
                "disc INTEGER," +
                "duration BIGINT," +
                "album TEXT," +
                "mod BIGINT);";
    }

    /**
     * Called when an entry is read from the database and is ready to be
     * parsed.
     * <p>
     * Note that this method is called for every row. Do NOT call {@link
     * ResultSet#next()}!
     *
     * @param result The result that is read from.
     */
    @Override
    protected LocalSong readResult(ResultSet result) throws SQLException
    {
        LocalSong song = new LocalSong();
        song.file = new File(result.getString("file"));
        song.codec = result.getString("codec");
        song.type = result.getString("type");
        song.title = result.getString("title");
        song.artists =
                Arrays.stream(result.getString("artists").split(";"))
                      .dropWhile(String::isEmpty)
                      .map(String::trim).toArray(String[]::new);
        song.trackNum = result.getInt("track");
        song.disc = result.getInt("disc");
        song.duration = result.getLong("duration");
        try
        {
            getAlbumProvider().joinUpdate();
            song.album = getAlbumProvider().getAlbumByName(result.getString(
                    "album"));
        }
        catch (InterruptedException e)
        {
            logger.error("Couldn't wait for album update for song {}", song, e);
        }
        song.lastMod = result.getLong("mod");
        return song;
    }

    /**
     * Called to obtain the properties of an object to write.
     *
     * @param item - The item to serialize.
     * @return Properties to write.
     */
    @Override
    protected Map<String, Object> serializeItem(LocalSong item)
    {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("file", item.file.getAbsolutePath());
        map.put("codec", item.codec);
        map.put("type", item.type);
        map.put("title", item.title);
        map.put("artists", Arrays.stream(item.artists).reduce("",
                (s1, s2) -> s1.isEmpty() ? s2 : s1 + ";" + s2));
        map.put("track", item.trackNum);
        map.put("disc", item.disc);
        map.put("duration", item.duration);
        map.put("album",
                Optional.ofNullable(item.album).map(a -> a.name).orElse(null));
        map.put("mod", item.lastMod);
        return map;
    }

    /**
     * Converts a piece of data into a string that will display in the update
     * text.
     *
     * @param data - The data to stringify.
     * @return A string representation of the data being updated.
     */
    @Override
    protected String stringifyResult(LocalSong data)
    {
        return Optional.ofNullable(data.album).map(a -> a.name).orElse(null) +
                "/" + data.title;
    }

    /**
     * A callback for when the database scan is complete.
     * <p>
     * Note that this method is called from the same thread that the scanner is
     * from.
     * </p>
     *
     * @return A fork-join task to invoke. This may be null.
     */
    @Override
    protected ForkJoinTask[] onComplete()
    {
        LinkedHashMap<Path, Map.Entry<ArrayList<Path>, ArrayList<Path>>> scanRoots =
                new LinkedHashMap<>();
        boolean existing;
        Map.Entry<ArrayList<Path>, ArrayList<Path>> subPaths;
        logger.debug("Searching for scan roots among {}", (Object) ConfigManager
                .getMusicDirs());
        for (Path toScan : ConfigManager.getMusicDirs())
        {
            existing = false;
            /*
             * Make sure that some scan roots aren't subfolders of other roots.
             */
            for (Path existingPath : scanRoots.keySet())
            {
                if (toScan.startsWith(existingPath))
                {
                    scanRoots.get(existingPath).getKey().add(toScan);
                    existing = true;
                    break;
                }
            }
            if (!existing)
            {
                /*
                 * Give each scanner a list of excluded folders that they
                 * will run across.
                 */
                subPaths = new AbstractMap.SimpleEntry<>(new ArrayList<>(),
                        new ArrayList<>());
                for (Path toExclude : ConfigManager.getMusicExcludeDirs())
                {
                    if (toScan.startsWith(toExclude))
                    {
                        subPaths.getValue().add(toExclude);
                        /*
                         * While we could ensure that we exclude subfolders
                         * of other excludes, there shouldn't be any
                         * performance impact from not doing so, and doing so
                         * would waste processing cycles.
                         */
                    }
                }
                scanRoots.put(toScan, subPaths);
            }
        }
        ArrayList<Path> toScan = new ArrayList<>();
        List<ForkJoinTask<List<Path>>> tasks =
                scanRoots.entrySet().stream()
                         .map(entry -> new FolderCounter(entry.getKey(),
                                 entry.getValue().getKey().toArray(Path[]::new),
                                 entry.getValue().getValue()
                                      .toArray(Path[]::new)))
                         .map(this.service::submit)
                         .collect(Collectors.toList());
        this.updating.set(-1);
        for (ForkJoinTask<List<Path>> task : tasks)
        {
            toScan.addAll(task.join());
        }
        this.updating.set(toScan.size());
        logger.debug("Scanning {} files", toScan.size());
        return toScan.stream().map(SongScanner::new)
                     .toArray(SongScanner[]::new);
    }

    private class FolderCounter extends RecursiveTask<List<Path>>
    {
        private final Path source;
        private final Path[] exclude;
        private final Path[] include;

        public FolderCounter(Path source, Path[] exclude, Path[] include)
        {
            this.source = source;
            this.exclude = exclude;
            this.include = include;
        }

        /**
         * The main computation performed by this task.
         *
         * @return the result of the computation
         */
        @Override
        protected List<Path> compute()
        {
            List<Path> results = new ArrayList<>();
            if (Files.isDirectory(this.source))
            {
                try
                {
                    List<FolderCounter> tasks =
                            Files.list(this.source).flatMap(f -> {
                                ArrayList<Path> toInclude = new ArrayList<>();
                                if (Arrays.binarySearch(this.exclude, f) > -1)
                                {
                                    /*
                                     * Just skip
                                     */
                                    for (Path include : this.include)
                                    {
                                        if (include.startsWith(f))
                                        {
                                            toInclude.add(include);
                                        }
                                    }
                                }
                                else
                                {
                                    toInclude.add(f);
                                }
                                return toInclude.stream();
                            }).filter(f -> {
                                if (Files.isDirectory(f))
                                {
                                    return true;
                                }
                                else
                                {
                                    results.add(f);
                                    return false;
                                }
                            }).map(f -> {
                                Path[] exclude =
                                        Arrays.stream(this.exclude)
                                              .filter(e -> e.startsWith(f))
                                              .toArray(Path[]::new);
                                Path[] include =
                                        Arrays.stream(this.include)
                                              .filter(e -> e.startsWith(f))
                                              .toArray(Path[]::new);
                                return new FolderCounter(f, exclude, include);
                            }).collect(Collectors.toList());
                    results.addAll(invokeAll(tasks).stream().flatMap(s -> {
                        try
                        {
                            return s.get().stream();
                        }
                        catch (InterruptedException | ExecutionException e)
                        {
                            logger.error("Could not get results for {}", s, e);
                            return Stream.empty();
                        }
                    }).collect(Collectors.toList()));
                }
                catch (IOException e)
                {
                    logger.error("Could not get subfolders of {}", this.source,
                            e);
                }
            }
            return results;
        }
    }

    private class SongScanner extends ForkJoinTask<LocalSong>
    {
        private final Path file;
        private LocalSong song;

        SongScanner(Path folder)
        {
            this.file = folder;
        }

        /**
         * Returns the result that would be returned by {@link #join}, even if
         * this task completed abnormally, or {@code null} if this task is not
         * known to have been completed.  This method is designed to aid
         * debugging, as well as to support extensions. Its use in any other
         * context is discouraged.
         *
         * @return the result, or {@code null} if not completed
         */
        @Override
        public LocalSong getRawResult()
        {
            return this.song;
        }

        /**
         * Forces the given value to be returned as a result.  This method is
         * designed to support extensions, and should not in general be called
         * otherwise.
         *
         * @param value the value
         */
        @Override
        protected void setRawResult(LocalSong value)
        {
            this.song = value;
        }

        @Override
        public boolean exec()
        {
            LocalSong existing;
            boolean update;

            existing =
                    getCollection().stream()
                                   .filter(f -> f.file.toPath()
                                                      .equals(this.file))
                                   .findFirst().orElse(null);
            if (existing != null)
            {
                /*
                 * The file has been updated.
                 */
                update = existing.file.lastModified() > existing.lastMod;
            }
            else
            {
                update = true;
            }
            if (update)
            {
                updateItem =
                        this.file.subpath(this.file.getNameCount() - 3,
                                this.file.getNameCount() - 1).toString();
                triggerUpdateListeners();
                try
                {
                    this.complete(this.readSong(this.file, existing));
                }
                catch (IOException | InterruptedException e)
                {
                    this.completeExceptionally(e);
                }
            }
            progress.incrementAndGet();
            triggerUpdateListeners();
            return this.isCompletedNormally();
        }

        private LocalSong readSong(Path path, LocalSong write) throws IOException, InterruptedException
        {
            Process process;
            String line, lineData;
            String[] streamData;
            Album album;
            String type;
            String codec = null;
            String[] genre = null;
            String title = null;
            String[] artist = null;
            String albumTitle = null;
            String[] albumArtist = null;
            int year = 0;
            long duration = 0;
            Integer[] track = null;
            Integer[] disc = null;

            type = path.getFileName().toString()
                       .substring(path.getFileName().toString()
                                      .lastIndexOf('.') + 1).toLowerCase();

            if (!getFormats().contains(type))
            {
                return null;
            }

            process = Runtime.getRuntime()
                             .exec(new String[]{"ffprobe", "-hide_banner",
                                     path.toAbsolutePath().toString()});
            process.waitFor();

            try (Scanner scanner = new Scanner(process.getErrorStream()))
            {
                while (scanner.hasNextLine())
                {
                    line = scanner.nextLine().trim();
                    try
                    {
                        final String data;
                        int index = line.indexOf(':');
                        if (index > 0 && index < line.length() - 2)
                        {
                            data = line.substring(index + 2);
                        }
                        else
                        {
                            data = line;
                        }
                        switch (line.toLowerCase()
                                    .substring(0, line
                                            .indexOf(' ') > 0 ? line
                                            .indexOf(' ') : line
                                            .length()))
                        {
                        case "genre" -> {
                            /*
                             * We only take the first one, as to
                             * avoid mishaps with labels after the
                             * metadata.
                             */
                            if (genre == null)
                            {
                                genre = Arrays.stream(data.split(";"))
                                              .map(String::trim)
                                              .toArray(String[]::new);
                            }
                        }
                        case "title" -> {
                            if (title == null)
                            {
                                title = data;
                            }
                        }
                        case "artist" -> {
                            if (artist == null)
                            {
                                artist = Arrays.stream(data.split(";"))
                                               .map(String::trim)
                                               .toArray(String[]::new);
                            }
                        }
                        case "album" -> {
                            if (albumTitle == null)
                            {
                                albumTitle = data;
                            }
                        }
                        case "album_artist" -> {
                            if (albumArtist == null)
                            {
                                albumArtist = Arrays.stream(data.split(";"))
                                                    .map(String::trim)
                                                    .toArray(String[]::new);
                            }
                        }
                        case "track" -> {
                            lineData = data;
                            if (lineData.indexOf('/') >= 0)
                            {
                                track = Arrays
                                        .stream(lineData
                                                .split("/"))
                                        .map(Integer::parseInt)
                                        .toArray(Integer[]::new);
                            }
                            else
                            {
                                if (track != null)
                                {
                                    track[0] = Integer
                                            .parseInt(lineData);
                                }
                                else
                                {
                                    track = new Integer[]{Integer.parseInt(lineData), -1};
                                }
                            }
                        }
                        case "tracktotal" -> {
                            if (track != null)
                            {
                                track[1] = Integer.parseInt(data);
                            }
                            else
                            {
                                track = new Integer[]{-1, Integer.parseInt(data)};
                            }
                        }
                        case "disc" -> {
                            lineData = data;
                            if (lineData.indexOf('/') >= 0)
                            {
                                disc = Arrays
                                        .stream(lineData
                                                .split("/"))
                                        .map(Integer::parseInt)
                                        .toArray(Integer[]::new);
                            }
                            else
                            {
                                if (disc != null)
                                {
                                    disc[0] = Integer
                                            .parseInt(lineData);
                                }
                                else
                                {
                                    disc = new Integer[]{Integer.parseInt(lineData), -1};
                                }
                            }
                        }
                        case "disctotal" -> {
                            if (disc != null)
                            {
                                disc[1] = Integer.parseInt(data);
                            }
                            else
                            {
                                disc = new Integer[]{-1, Integer.parseInt(data)};
                            }
                        }
                        case "date" -> {
                            lineData = line.substring(line
                                            .indexOf(':') + 2,
                                    Math.min(line.indexOf(':') + 6,
                                            line.length())).trim();
                            year = Integer.parseInt(lineData);
                        }
                        case "duration:" -> {
                            if (duration == 0)
                            {
                                lineData = line.substring(line
                                        .indexOf(':') + 2, line
                                        .indexOf(','));
                                if (!lineData.equals(
                                        "N/A"))
                                {
                                    duration = Long
                                            .parseLong(lineData
                                                    .substring(0, 2)) * 3600 * 1000 + Long
                                            .parseLong(lineData
                                                    .substring(3, 5)) * 60 * 1000 + Long
                                            .parseLong(lineData
                                                    .substring(6, 8)) * 1000 + (long) (Float
                                            .parseFloat(lineData
                                                    .substring(8, lineData
                                                            .length() - 1)) * 1000);
                                }
                            }
                        }
                        case "stream" -> {
                            streamData = line.split(" ");
                            if (streamData[2].equals("Audio:"))
                            {
                                codec = streamData[3];
                                if (codec.endsWith(","))
                                {
                                    codec = codec
                                            .substring(0, codec
                                                    .length() - 1);
                                }
                                /*
                                 * If this isn't a supported codec,
                                 * discard.
                                 */
                                if (!getCodecs()
                                        .contains(codec))
                                {
                                    logger.trace("Invalid codec {} for song {}", codec, file);
                                    codec = null;
                                }
                                else
                                {
                                    logger.trace("Found codec {} for song {}", codec, file);
                                }
                            }
                            else
                            {
                                logger.trace("Found non-audio stream {} for {}", line, file);
                            }
                        }
                        }
                    }
                    catch (NumberFormatException e)
                    {
                        throw new RuntimeException(
                                "Could not parse line \"" + line + "\"", e);
                    }
                }
            }

            if (codec == null)
            {
                return null;
            }

            if (write == null)
            {
                write = new LocalSong();
                write.file = path.toFile();
                write.codec = codec;
                write.type = type;
            }

            write.lastMod = Files.getLastModifiedTime(path).toMillis();

            write.title = title;
            if (artist != null && artist.length > 0)
            {
                write.artists = artist;
            }
            if (duration > 0)
            {
                write.duration = duration;
            }
            if (track != null && track.length > 0)
            {
                write.trackNum = track[0];
            }
            if (disc != null && disc.length > 0)
            {
                write.disc = disc[0];
            }

            getAlbumProvider().joinUpdate();
            album = albums.getAlbumByName(albumTitle);
            if (album == null)
            {
                album = new Album();
                album.name = albumTitle;
            }
            if (albumArtist != null && albumArtist.length > 0)
            {
                album.artists = albumArtist;
            }
            if (genre != null && genre.length > 0)
            {
                album.genres = genre;
            }
            if (year > 0)
            {
                album.year = year;
            }
            if (track != null && track.length > 1)
            {
                album.totalTracks = track[1];
            }
            if (disc != null && disc.length > 1)
            {
                album.totalDiscs = disc[1];
            }
            write.album = album;

            getAlbumProvider().writeItem(album);
            writeItem(write);

            return write;
        }
    }
}
