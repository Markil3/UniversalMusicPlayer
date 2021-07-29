console.log("Hello from Universal Music addon!")
/*
On startup, connect to the "ping_pong" app.
*/
var port = browser.runtime.connectNative("universalmusic");

var listeners = [function (message, returnValue) {
    if (returnValue == null)
    {
        returnValue = "pong";
    }
    return returnValue;
}];

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function handleMessage(message)
{
    return new Promise((resolve, reject) => {
        var returnValue = null;
        for (index in listeners)
        {
            returnValue = listeners[index](message, returnValue);
        }
        resolve(returnValue);
    })
}

/*
 * Listen for messages from the app.
 */
port.onMessage.addListener((message) => {
    console.log("Received: ", message);

    handleMessage(message.message).then((response) => {
        returnValue = {
            "messageNum": message.messageNum,
            "message": response
        }
        console.log("Sending ", returnValue)
        port.postMessage(returnValue);
    });
});
