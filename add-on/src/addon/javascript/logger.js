/**
 * Message data is used internally as a wrapper for a log message that is sent to the interface.
 */
class MessageData {
    /**
     * This is used by the interface parser to determine what type of data this is.
     */
    type = "edu.regis.universeplayer.Log";

    /**
     * This contains the name of the logger that created the message.
     */
    logger;

    /**
     * The log level this message was sent at.
     * @type string
     */
    level;

    /**
     * The actual message sent, as an array of arguments.
     * @type array
     */
    message;

    /**
     * Creates a message package.
     *
     * @param {string} level        The log level.
     * @param {array-like} message  The message to send, as an array-like object.
     */
    constructor(logger, level, message)
    {
        this.logger = logger;
        this.level = level;
        this.message = Array.from(message);
    }
}

class Logger {
    name;
    /**
     * Messages that are scheduled to be sent.
     */
    queuedMessages = [];

    constructor(name)
    {
        this.name = name;
    }

    /**
     * This will format the message in a way SLF4j can understand the log message before scheduling
     * it for forwarding.
     */
    passLog(level, message)
    {
        let messageData;
        if (message.length > 1 && typeof message[0] == "string")
        {
            /*
             * Format the initial string in a way that SLF4J can understand it.
             */
            message[0] = message[0].replaceAll(/%[a-z]/gi, "{}");
        }
        this.queuedMessages.push(new MessageData(this.name, level, message));
        message = this.queuedMessages.pop();
        while (message && this.pushUpdate(message))
        {
            message = this.queuedMessages.pop();
        }
        /*
         * If we popped a message but got here, then a message was not sent. Put it back.
         */
        if (message)
        {
            this.queuedMessages.unshift(message);
        }
    }

    /**
     * This method is used to actually push data to the interface. Abstract.
     *
     * @param [MessageData] message The message to push.
     * @return [boolean]            Whether or not the push was successful.
     */
    pushUpdate(message)
    {
        return false;
    }

    trace()
    {
        console.trace.apply(console, arguments);
        this.passLog("trace", arguments);
    }
    debug()
    {
        console.debug.apply(console, arguments);
        this.passLog("debug", arguments);
    }
    info()
    {
        console.info.apply(console, arguments);
        this.passLog("info", arguments);
    }
    log()
    {
        console.log.apply(console, arguments);
        this.passLog("info", arguments);
    }
    warn()
    {
        console.warn.apply(console, arguments);
        this.passLog("warn", arguments);
    }
    error()
    {
        console.error.apply(console, arguments);
        this.passLog("error", arguments);
    }
}