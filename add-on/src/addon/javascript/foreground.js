let logger = new Logger("foreground");
console.debug("Loading foreground.js");
let background;

/**
 * Contains functions to call before establishing a connection.
 */
let preload = [];

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

/**
 * Handles messages sent from the background script.
 *
 * @param {object} message -The object containing the request.
 * @return {Promise}        A promise containing the result of the request.
 */
function handleMessage(message)
{
    if (typeof message == "object" && "type" in message)
    {
        let type = message.type;
        lastPeriod = type.lastIndexOf(".");
        if (lastPeriod != -1)
        {
            type = type.substring(lastPeriod + 1);
        }
        switch (type)
        {
        case "NumberPing":
            logger.log("Pinging back %d from foreground", message.number);
            return Promise.resolve(message.number);
        case "QuerySongData":
            logger.log("Gathering song data");
            return getSongData();
        case "QueryStatus":
            return getState();
        case "QueryTime":
            return getTime();
        case "QueryLength":
            return getLength();
        case "CommandSetPlayback":
            switch (message.status)
            {
            case "PLAY":
                return play();
            case "PAUSE":
                return pause();
            }
            return false;
        case "CommandSeek":
            return seek(message.time);
        case "CommandError":
            throw new TypeError("This is a foreground test");
            return false;
        }
    }
}

/**
 * Calling this function will forward playback data to the browser background (and by extension, the
 * interface) as specified by the parameters.
 *
 * @param {string} status   The playback status. This can be "PLAYING", "PAUSED", "STOPPED",
 *                          "FINISHED", or "EMPTY"
 * @param {number} time     The current playback time in seconds.
 * @param {object} songData Data on the song currently playing.
 */
function onStatusUpdate(status, time, songData)
{
    sendUpdate({
        type: "edu.regis.universeplayer.PlaybackInfo",
        currentSong: songData,
        status: status,
        playTime: time
    });
}

/**
 * Forwards an update to the background (and by extension, the interface).
 *
 * @param {object|Promise} response The data to forward. If this data is a Promise, then the data
 *                                  will be forwarded upon completion.
 */
function sendUpdate(response)
{
    let post = data => background.postMessage({
        type: "update",
        data: data
    });
    if (response instanceof Promise)
    {
        response.then(post);
    }
    else
    {
        post(response);
    }
}

$(function () {
    while (preload.length > 0)
    {
        preload.pop()();
    }
    background = browser.runtime.connect({name:"universalMusic"});

//    sendUpdate("loaded");

    /**
     * Listens for messages from the browser background and sends responses back.
     */
    background.onMessage.addListener(message => {
        let num = message.num;
        post = data => {
            logger.trace("Sending to interface %o", data);
            background.postMessage({
                type: "response",
                num: num,
                data: data
            });
        }
        try
        {
            let response = handleMessage(message.data);
            if (response instanceof Promise)
            {
                logger.log("Posting promised information.");
                response.then(post);
            }
            else
            {
                logger.log("Posting non-promise information.");
                post(response);
            }
        }
        catch (e)
        {
            logger.error(e);
            post(e);
        }
    });
})

/**
 * Obtains information on the song currently playing.
 *
 * @return {Promise<InternetSong>} A serialized form of the song data for this tab.
 */
function getSongData()
{
    return Promise.reject(new ReferenceError("getSongData unimplemented for " + location.href));
}

/**
 * Obtains the current tab's playback state.
 *
 * @return {Promise<string>}    The playback status. This can be "PLAYING", "PAUSED", "STOPPED",
 *                              "FINISHED", or "EMPTY"
 */
function getState()
{
    return Promise.reject(new ReferenceError("getState unimplemented for " + location.href));
}

/**
 * Obtains the current playback time.
 *
 * @return {Promise<number>}    The current song playback time in milliseconds.
 */
function getTime()
{
    return Promise.reject(new ReferenceError("getTime unimplemented for " + location.href));
}

/**
 * Obtains the total length of the song.
 *
 * @return {Promise<number>}    The total song length, in milliseconds.
 */
function getLength()
{
    return Promise.reject(new ReferenceError("getLength unimplemented for " + location.href));
}

/**
 * Enables playback of the current song.
 *
 * @return {Promise}    The success of the command.
 */
function play()
{
    return Promise.reject(new ReferenceError("play unimplemented for " + location.href));
}

/**
 * Pauses playback of the current song.
 *
 * @return {Promise}    The success of the command.
 */
function pause()
{
    return Promise.reject(new ReferenceError("play unimplemented for " + location.href));
}

/**
 * Halts playback of the current song.
 *
 * @return {Promise}    The success of the command.
 */
function stop()
{
    return Promise.reject(new ReferenceError("stop unimplemented for " + location.href));
}

/**
 * Sets the playback to a certain time.
 *
 * @param {number} time The time to skip to, in milliseconds.
 * @return {Promise}    The success of the command.
 */
function seek(time)
{
    return Promise.reject(new ReferenceError("seek unimplemented for " + location.href));
}