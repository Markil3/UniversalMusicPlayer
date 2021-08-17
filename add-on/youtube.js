console.debug("Loading youtube.js")
var video;

function onload()
{
    video = document.getElementsByClassName('video-stream html5-main-video')[0]
}

function getState()
{
    if (!video)
    {
        return 4;
    }
    else if (video.ended)
    {
        return 3;
    }
    else if (video.paused)
    {
        return 1;
    }
    else
    {
        return 0;
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