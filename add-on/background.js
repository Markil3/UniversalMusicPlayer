console.log("Hello from Universal Music addon!")
const tabMessages = new Map();
var numTabMessages = 0;
/*
On startup, connect to the "ping_pong" app.
*/
var port = browser.runtime.connectNative("universalmusic");

var tabPort = null;

function setupTab(port)
{
    console.info("Tab " + port.sender.tab.url + " loaded.");
    tabPort = port;
    tabPort.onDisconnect.addListener(e => {
        tabPort = null;
        if (e.error)
        {
            console.error("Tab error: ", e.error)
        }
        else
        {
            console.info("Tab " + e.sender.tab.url + " disconnected.");
        }
        browser.tabs.remove(e.sender.tab.id);
    });
    tabPort.onMessage.addListener(message => {
        if (typeof message == "object" && message.type == "response")
        {
            tabMessages.set(message.num, message.data);
        }
    });
    return true;
}

function queryTab(message)
{
    let num = numTabMessages++;
    let promise = new Promise((resolve, reject) => {
        tabPort.postMessage({
            num: num,
            data: message
        });
        (function awaitResponse() {
            if (tabMessages.has(num))
            {
                response = tabMessages.get(num);
                tabMessages.delete(num);
                return resolve(response);
            }
            else
            {
                setTimeout(awaitResponse, 30);
            }
        })();
    });
    tabMessages.set(numTabMessages, promise);
    numTabMessages++;
    return promise;
}

var listeners = [function (message, returnValue) {
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
        case "CommandSong":
            returnValue = () => browser.tabs.create({url: message.song}).then(setupTab);
            if (tabPort)
            {
                console.log("Replacing tab");
                /*
                 * Closes the existing tab first
                 */
                returnValue = browser.tabs.remove(e.sender.tab.id).then(returnValue);
            }
            else
            {
                console.log("Opening tab");
                returnValue = returnValue();
            }
            break;
        case "QueryStatus":
            if (!tabPort)
            {
                returnValue = 4;
                break;
            }
        case "QueryTime":
        case "QueryLength":
        case "CommandSetPlayback":
        case "CommandSeek":
            returnValue = queryTab(message);
            break;
        case "CommandQuit":
            returnValue = quit();
            break;
        }
    }
    else if (message == "quit")
    {
        returnValue = quit();
    }
    if (returnValue == null)
    {
        returnValue = "pong";
    }
    return returnValue;
}];

function quit()
{
    console.log("Quitting browser");
    return browser.tabs.query({}).then(tabs => {
        for (let tab of tabs)
        {
            browser.tabs.remove(tab.id);
        }
    });
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function handleMessage(message)
{
    return new Promise((resolve, reject) => {
        var returnValue = null;
        try
        {
            for (index in listeners)
            {
                returnValue = listeners[index](message, returnValue);
            }
            if (returnValue instanceof Promise)
            {
                returnValue.then(resolve).catch(reject);
            }
            else
            {
                resolve(returnValue);
            }
        }
        catch (e)
        {
            reject(e);
        }
    })
}

/*
 * Listen for messages from the app.
 */
port.onMessage.addListener((message) => {
    console.log("Received from interface: ", message);

    handleMessage(message.message).then((response) => {
        returnValue = {
            "messageNum": message.messageNum,
            "message": {
                "type": "edu.regis.universeplayer.browserCommands.CommandReturn",
                "returnValue": response,
                "confirmation": {
                    type: "edu.regis.universeplayer.browserCommands.CommandConfirmation",
                    message: "Done",
                    errorCode: null
                }
            }
        }
        console.log("Sending ", returnValue)
        port.postMessage(returnValue);
    }).catch(error => {

    });
});

browser.runtime.onConnect.addListener(setupTab);
