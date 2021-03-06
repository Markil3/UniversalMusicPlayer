let ylogger = new Logger("youtube");

logger.pushUpdate = function (message)
{
    if (background)
    {
        sendUpdate(message);
        return true;
    }
    else
    {
        return false;
    }
};

console.debug("Loading youtube.js")
var video;

function onload()
{
    ylogger.info("YouTube loaded");
    $(".more-button").click();
    video = $('.video-stream.html5-main-video')[0];
    statusUpdate = e => {
        return onStatusUpdate(getState(), e.srcElement.currentTime, getSongData());
    };
//    video.addListener("timeupdate", statusUpdate);
//    video.addListener("play", statusUpdate);
//    video.addListener("pause", statusUpdate);
//    video.addListener("ended", statusUpdate);
    video.ontimeupdate = statusUpdate;
    video.onplay = statusUpdate;
    video.onpause = statusUpdate;
    video.onended = statusUpdate;
}

function getSongData()
{
    let song = getAutogeneratedMetadata();
    if (!song)
    {
        song = getDetectedMetadata();
        if (!song)
        {
            song = getBasicMetadata();
        }
    }
    return Promise.resolve(song);
}

/**
 * Obtains basic song metadata for if nothing else is detected.
 */
function getBasicMetadata()
{
    let data = new InternetSong();
    data.location = window.location.href;
    data.title = getTitle();
    data.artists = getArtists();
    data.duration = getLength();

    data.album = new Album();
    data.album.art = getArt();

    return Promise.resolve(data);
}

/**
 * Obtains song metadata detected by YouTube.
 *
 * @param {string} type             The type of metadata to get. Can be "Song", "Artist", "Album",
 *                                  "Writers", or "Licensed to YouTube by". May be null.
 * @return {jQuery|InternetSong}    If a type is provided, this returns a jQuery array of string
 *                                  values matching that type. Otherwise, it returns an InternetSong
 *                                  created from detected metadata.
 */
function getDetectedMetadata(type)
{
    if (type)
    {
        return $("ytd-metadata-row-container-renderer.ytd-video-secondary-info-renderer ytd-metadata-row-renderer:contains('" + type + "') div>yt-formatted-string").map(function () {return $(this).text()});
    }
    else
    {
        let song = new InternetSong();

        song.title = Array.prototype.join.apply(getDetectedMetadata("Song"), [" / "]);
        if (!song.title)
        {
            return undefined;
        }
        song.artists = ";".join(getDetectedMetadata("Artist"));
        song.album.name = getDetectedMetadata("Album")[0];
        song.album.artists = ";".join(getDetectedMetadata("Artist"));
        song.location = location.href;
        song.duration = getLength();
        song.album.art = getArt();

        return song;
    }
}

/**
 * Obtains song metadata detected by YouTube as provided in the description.
 *
 * @return {InternetSong}   The internet song data detected. This will be undefined if no
 *                          autogenerated data was found.
 */
function getAutogeneratedMetadata()
{
    let desc = $("yt-formatted-string.ytd-video-secondary-info-renderer").text().replaceAll(/\n\w*\n/g, "\n").split("\n");
    if (!desc || !desc.length || !desc[desc.length - 1].startsWith("Auto-generated"))
    {
        return undefined;
    }
    let data = new InternetSong();
    desc.forEach(str => {
        let provHead = "Provided to YouTube by ";
        let names;
        if (str.startsWith(provHead))
        {
            data.provider = str.substring(provHead.length);
        }
        else if (str.indexOf(" ?? ") > -1)
        {
            names = str.split(" ?? ");
            data.title = names[0];
            data.artists = names.slice(1);
            if (!data.album.artists)
            {
                data.album.artists = data.artists;
            }
        }
        else if (str.startsWith('???'))
        {
            data.album.artists = [str.substr(str.match(/^???( [\d]{4})? /)[0].length)]
            names = str.match(/\d{4}/);
            if (!data.album.year && names)
            {
                data.album.year = names[0];
            }
        }
        else if (str.startsWith('Released on: '))
        {
            names = str.match(/[\d]{4}/);
            if (names)
            {
                data.album.year = names[0];
            }
        }
        else if (data.title && !data.album.name && str)
        {
            ylogger.debug("Album name: %s", str);
            data.album.name = str;
        }
        else
        {
            ylogger.debug("Ignoring line %s", str);
        }
    });
    data.location = location.href;
    data.duration = getLength();
    data.album.art = getArt();
    return data;
}

/**
 * Obtains the song title.
 *
 * @return {string} The song title, or null if one could not be found.
 */
function getTitle()
{
    return $(".title.ytd-video-primary-info-renderer").text()
}

/**
 * Obtains the artists who made this song.
 *
 * @return {string[]}   The song artists, or an empty array if none could be found.
 */
function getArtists()
{
    return [$("#channel-name.ytd-video-owner-renderer a:first").text()];
}

/**
 * Obtains the YouTube ID for this song
 *
 * @return {string}
 */
function getSongId()
{
    let match = location.search.match(/v=.+(\&|$)/);
    if (match)
    {
        return match[0].substring(2)
    }
    return "";
}

/**
 * Obtains the album art
 *
 * @return {blob|string} Either a blob for the album art or a URL for where it can be downloaded.
 */
function getArt()
{
    return "https://i.ytimg.com/vi/" + getSongId() + "/maxresdefault.jpg"
}

function getState()
{
    if (!video)
    {
        return "EMPTY";
    }
    else if (video.ended)
    {
        return "FINISHED";
    }
    else if (video.paused)
    {
        return "PAUSED";
    }
    else
    {
        return "PLAYING";
    }
}

function getTime()
{
    return parseInt(video.currentTime * 1000);
}

function getLength()
{
    return parseInt(video.duration * 1000);
}

function play()
{
    if (video != null)
    {
        video.play();
        return true;
    }
    return false;
}

function pause()
{
    if (video != null)
    {
        video.pause();
        return true;
    }
    return false;
}

function seek(time)
{
    if (video != null)
    {
        video.currentTime = time;
        return true;
    }
    return false;
}

preload.push(onload);