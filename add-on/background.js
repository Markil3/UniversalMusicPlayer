console.log("Hello from Universal Music addon!")
/*
On startup, connect to the "ping_pong" app.
*/
var port = browser.runtime.connectNative("universalmusic");

/*
Listen for messages from the app.
*/
port.onMessage.addListener((response) => {
  console.log("Received: " + response);
  port.postMessage("pong");
});
