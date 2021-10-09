let logger = new Logger("background");

logger.pushUpdate = function (message)
{
    if (interfacePort)
    {
        interfacePort.postMessage({
            "messageNum": -1,
            "message": message
        });
        return true;
    }
    else
    {
        return false;
    }
};

logger.log("Hello from Universal Music addon!")

/**
 * This variable contains a mapping of message IDs to the message promises they correspond to, as
 * long as those messages are still pending a response.
 */
const tabMessages = new Map();

/**
 * This counter variable is used to generate session-unique message IDs between this background and
 * the various tabs.
 */
var numTabMessages = 0;

/*
On startup, connect to the "ping_pong" app.
*/
var interfacePort = browser.runtime.connectNative("universalmusic");

/**
 * This variable contains a mapping between tab IDs and resolution functions that are to be called
 * as soon as a connection port is established.
 */
let waitingPorts = new Map();

/**
 * This variable contains a mapping between tab IDs and respective communication ports.
 */
let ports = new Map();

/**
 * Obtains the host name of a URL
 */
function getHost(url)
{
    return url.replace(/^\w+:\/{2,3}/, "").replace(/[\/#?:]\w+.+$/, "")
}

/**
 * Obtains the domain of a URL
 */
function getDomain(url)
{
    let host = getHost(url);
    let parts = host.split(".");
    return parts[parts.length - 2] + "." + parts[parts.length - 1];
}

/**
 * Loads up a tab and returns a promise for when it has completely loaded.
 *
 * @param {string} url      The URL to load.
 * @param {boolean} pinned  Whether the tab should be pinned or not. Pinned tabs can be reused.
 *                          Defaults to true.
 * @return {Promise}        A promise that loads the tab. The resolution function is passed the tab
 *                          object upon completion.
 */
function loadTab(url, pinned)
{
    if (typeof pinned != "boolean" && !pinned)
    {
        pinned = true;
    }
    let chosen;
    return new Promise((resolve, reject) => {
        let domain = getDomain(url);

        browser.tabs.query({
            url: "*://*." + domain + "/*",
            muted: false
        }).then(tabs => {
            let callback = (tabId, changed, tab) => {
                if (tab.status == "complete")
                {
                    logger.debug("Tab %d complete", tab.id);
                    if (browser.tabs.onUpdated.hasListener(callback))
                    {
                        browser.tabs.onUpdated.removeListener(callback)
                    }
                    resolve(tab);
                }
            };
            let navigate = (tabId, changed, tab) => {
                if (tab.status == "complete")
                {
                    if (browser.tabs.onUpdated.hasListener(navigate))
                    {
                        browser.tabs.onUpdated.removeListener(navigate)
                    }
                    chosen = tabId;
                    logger.debug("Redirecting tab %d %o", tabId, tab);
                    /*
                     * Reloading the tab disconnects the port. We will have to wait for a
                     * reconnection.
                     */
                    waitingPorts.set(tab.id, {
                        "resolve": callback,
                        "reject": reject
                    });
//                    browser.tabs.onUpdated.addListener(callback, {
//                        urls: [url],
//                        properties: ["status"]
//                    });
                    browser.tabs.update(tabId, {
                        url: url
                    }).then(() => {}, reject);
                }
            };
            let tab;

            /*
             * If there are no available tabs (or if we are instructed to create a new tab for this
             * task), then create a new tab and wait for a connection to be established.
             */
            logger.log("Found %o tabs", tabs);
            if (tabs.length == 0 || !pinned)
            {
//                browser.tabs.onUpdated.addListener(callback, {
//                    urls: [url],
//                    properties: ["status"]
//                });
                browser.tabs.create({
                    url: url
                }).then(tab => {
                    chosen = tab.id;
                    waitingPorts.set(tab.id, {
                        "resolve": callback,
                        "reject": reject
                    });
                    browser.tabs.update(tab.id, {
                        muted: !pinned
                    });
                }, reject);
            }
            /*
             * Otherwise, use an existing tab and set it to a new URL.
             */
            else
            {
                let i = 0;
                while (tabs[i].mutedInfo.muted)
                {
                    i++;
                }
                tab = tabs[i];
                if (tab.status == "loading")
                {
                    /*
                     * Only navigate if the tab has finished its task already.
                     */
                    browser.tabs.onUpdated.addListener(navigate, {
                        urls: [tab.url],
                        properties: ["status"],
                        tabId: tab.id
                    });
                }
                else
                {
                    navigate(tab.id, {}, tab);
                }
            }
        }, reject);
    }).finally(() => {
        if (!pinned)
        {
            browser.tabs.remove(chosen);
        }
    });
}

/**
 * This callback is called every time a new tab attempts to connect to this background process.
 *
 * @param {runtime.Port} port - The port object that the tab will communicate through.
 * @return {boolean}            true if the connection was successful, false otherwise.
 */
function setupTab(port)
{
    if (port.sender)
    {
        logger.info("Tab " + port.sender.tab.url + " loaded.");
    }
    else
    {
        logger.info("Tab loaded");
    }
    ports.set(port.sender.tab.id, port);
    if (waitingPorts.has(port.sender.tab.id))
    {
        console.log("Setting up port %o", port.sender.tab);
        waitingPorts.get(port.sender.tab.id).resolve(port.sender.tab.id, {}, port.sender.tab);
        waitingPorts.delete(port.sender.tab.id);
    }
    else
    {
        logger.warn("We just connected to a tab that we didn't request.");
    }
    port.onDisconnect.addListener(e => {
        ports.delete(e.sender.tab.id);
        if (e.error)
        {
            logger.error("Tab error: %o", e.error)
        }
        else
        {
            logger.info("Tab " + e.sender.tab.url + " disconnected.");
        }
//        browser.tabs.remove(e.sender.tab.id);
    });

    /**
     * Listens for messages from the tab and processes their promises
     */
    port.onMessage.addListener(message => {
        let returnValue;
        if (typeof message == "object")
        {
            if (message.type == "response")
            {
                returnValue = tabMessages.get(message.num);
                logger.trace("Returning message %d, %o", message.num, returnValue)
                if (returnValue)
                {
                    tabMessages.delete(message.num);
                    if (message.data instanceof Error)
                    {
                        returnValue.reject(message.data);
                    }
                    else
                    {
                        returnValue.resolve(message.data);
                    }
                }
                else
                {
                    logger.warn("Couldn't find message number %d", message.num);
                }
            }
            else if (message.type == "update")
            {
                returnValue = {
                    "messageNum": -1,
                    "message": message.data
                }
                logger.trace("Sending update %o", returnValue)
                interfacePort.postMessage(returnValue);
            }
            else
            {
                logger.warn("Unknown message type %s", message.type);
            }
        }
        else
        {
            logger.warn("Unknown message type %s", typeof message, message);
        }
    });
    return true;
}

/**
 * Sends a message to specified tab and handles the response.
 * @param {id,string,tabs.Tab} tab -The tab to send to. This can be the ID of the tab, the hostname
 *                                  or domain name of the tab, an array of url matchers, or the tab
 *                                  object itself.
 * @return {Promise}                A promise that passes the tab's response to the resolver.
 */
function queryTab(tab, message)
{
    let num = numTabMessages++;

    let tabId;
    if (typeof tab == "string")
    {
        if (tab.match(/^\w+\.\w+$/))
        {
            tab = "*://*." + tab + "/*";
        }
        else if (tab.match(/^(\w\.)+\w+\.\w+$/))
        {
            tab = "*://" + tab + "/*";
        }
        tab = [tab];
    }
    else if ("id" in tab)
    {
        tab = tab.id;
    }

    return new Promise((resolve, reject) => {
        let query = port => {
            let messageReturn = {
                resolve: resolve,
                reject: reject
            };
            tabMessages.set(num, messageReturn);
            logger.log("Querying tab %o", message);
            port.postMessage({
                num: num,
                data: message
            })
        };

        if (typeof tab == "number")
        {
            if (ports.has(tab))
            {
                query(ports.get(tab));
            }
            else
            {
                reject(new ReferenceError("Could not find tab with communications port under " + tab));
            }
        }
        else if (tab instanceof Array)
        {
            tabs = tabs.query({
                url: tab,
                status: "complete"
            }).then(tabs => {
                if (tabs && tabs.length > 0)
                {
                    for (let i = 0; i < tabs.length; i++)
                    {
                        if (ports.has(tabs[i].id))
                        {
                            query(ports.get(tabs[i].id));
                            return;
                        }
                    }
                    /*
                     * We only reach here if we couldn't find a valid tab. While we could just fall
                     * through to the other reject statement, this one can provide a clearer error
                     * message.
                     */
                    reject(new ReferenceError("Could not find tab with communications under " + tab));
                }
                else
                {
                    reject(new ReferenceError("Could not find tab under " + tab));
                }
            }, reject);
        }
    });
}

/**
 * A callback for when the native application sends a message.
 *
 * @param {object} message -    The message to process.
 * @param {object} returnValue -The value returned by the last message listener. Return either this
 *                              or a new value.
 * @return Either the value passed on returnValue or a new value.
 */
var listeners = [function (message, returnValue) {
    if (typeof message == "object" && "type" in message)
    {
        let type = message.type;
        let lastPeriod = type.lastIndexOf(".");
        if (lastPeriod != -1)
        {
            type = type.substring(lastPeriod + 1);
        }
        switch (type)
        {
        case "NumberPing":
            if (message.url)
            {
                let tabId;
                return loadTab(message.url).then(tab => {
                    tabId = tab.id;
                    return queryTab(tab, message);
                }).then(data => {
//                    browser.tabs.remove(tabId);
                    return data;
                });
            }
            else
            {
                logger.log("Pinging back %d from background", message.number);
                returnValue = message.number;
            }
            break;
        case "QuerySongData":
            return loadTab(message.url, false).then(tab => {
                logger.log("Tab " + tab.url + " loaded. Querying.");
                return queryTab(tab, message);
            });
        case "CommandLoadSong":
            returnValue = () => {
                if (message.song)
                {
                    browser.tabs.create({url: message.song});
                }
            }
            if (tabPort)
            {
                logger.log("Replacing tab");
                /*
                 * Closes the existing tab first
                 */
                returnValue = browser.tabs.remove(tabPort.sender.tab.id).then(returnValue);
            }
            else
            {
                logger.log("Opening tab");
                returnValue = returnValue();
            }
            break;
        case "QueryStatus":
            if (!tabPort)
            {
                returnValue = "EMPTY";
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
        case "CommandError":
            logger.error("Error message %o", message);
            if (message.forward)
            {
                returnValue = queryTab(message);
            }
            else
            {
                throw new TypeError("This is a background test");
                returnValue = false;
            }
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

/**
 * Closes all tabs in the browser, thus closing the entire browser.
 *
 * @return {Promise} The promise that closes the tabs.
 */
function quit()
{
    logger.log("Quitting browser");
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

/**
 * This method is triggered when the interface sends a message to the browser. This method will
 * trigger all of the relevant message listeners and return a promise for whichever value they
 * decide to output.
 *
 * @param {object} message -The message to analyze.
 * @return {Promise}        A promise for the method completion, resolving to a value to return to
 *                          the interface.
 */
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
            logger.debug("Message returned: %o", returnValue);
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
interfacePort.onMessage.addListener((message) => {
    logger.log("Received from interface: %o", message);

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
        logger.log("Sending %o", returnValue)
        interfacePort.postMessage(returnValue);
    }).catch(error => {
//        logger.error("Error in evaluating message: %o", error);
        interfacePort.postMessage({
            "messageNum": message.messageNum,
            "message": {
                "type": "edu.regis.universeplayer.browserCommands.CommandReturn",
                "returnValue": null,
                "confirmation": {
                    type: "edu.regis.universeplayer.browserCommands.CommandConfirmation",
                    message: "Error in executing request",
                    errorCode: {
                        type: "edu.regis.universeplayer.browserCommands.BrowserError",
                        name: error.name,
                        message: error.message,
                        stack: error.stack
                    }
                }
            }
        });
    });
});

browser.runtime.onConnect.addListener(setupTab);

logger.log("Background script setup complete!")
