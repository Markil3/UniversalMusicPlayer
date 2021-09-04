console.debug("Loading youtube.js")
var video;

function onload()
{
    video = document.getElementsByClassName('video-stream html5-main-video')[0];
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
    return {
        type: "edu.regis.universeplayer.browser.InternetSong",
        location: "window.location.href",
        title: getTitle(),
        artists: getArtists(),
        trackNum: 0,
        discNum: 0,
        duration: parseInt(getLength() * 1000),
        album: null
    }
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
    return video.currentTime;
}

function getLength()
{
    return video.duration;
}

function getTitle()
{
    return document.getElementsByTagName("meta").title.content;
}

function getArtists()
{
    return [document.getElementById("channel-name").getElementsByTagName("a")[0].text];
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

if (document.readyState === "complete")
{
    onload();
}
else
{
    window.addEventListener("load", onload);
}