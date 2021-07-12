var port = browser.runtime.connectNative("universal_music")

/**
 * Sends a message to the application
 */
function writeMessage(message)
{
    port.postMessage(message)
}

/**
 * Sends a message to the application
 */
function onAppMessage(message)
{
    if ("command" in message)
    {
        switch (message["command"])
        {
        case "ping":
            writeMessage({
                "response": "ping"
            });
            break
        }
    }
}

port.onMessage.addListener(onAppMessage);