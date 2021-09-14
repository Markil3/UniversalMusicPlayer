console.debug("Loading foreground.js")
let background;

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

function onStatusUpdate(status, time, songData)
{
    sendUpdate({
        type: "edu.regis.universeplayer.PlaybackInfo",
        currentSong: songData,
        status: status,
        playTime: time
    });
}

function sendUpdate(response)
{
    post = data => background.postMessage({
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

background = browser.runtime.connect({name:"universalMusic"});
background.onMessage.addListener(message => {
    let num = message.num;
    response = handleMessage(message.data);
    post = data => background.postMessage({
        type: "response",
        num: num,
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
});

if (document.readyState === "complete")
{
    sendUpdate("loaded");
}
else
{
    sendUpdate("loaded");
    window.addEventListener("load", () => {
        sendUpdate("loaded");
    });
}