/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

import java.io.Serializable;

/**
 * A browser error is a special Throwable that contains information about an
 * error that occurred within the browser. This is so that we can properly log
 * browser errors under standard Java logging.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class BrowserError extends Throwable
{
    private final String name;
    private final String message;
    
    /**
     * Creats a browser error.
     *
     * @param name       - The type of error thrown.
     * @param message    - The message as reported by the browser.
     * @param stackTrace - The stack trace string provided by the browser.
     */
    public BrowserError(String name, String message, String stackTrace)
    {
        super(name + ": " + message, null, false, true);
        this.name = name;
        this.message = message;
        this.setStackTrace(this.parseFirefoxTrace(stackTrace));
    }
    
    /**
     * Gets the type of error returned by the browser.
     *
     * @return - The browser error type.
     */
    public String getType()
    {
        return this.name;
    }
    
    /**
     * Gets the original message from the browser.
     *
     * @return - The browser error message.
     */
    public String getBrowserMessage()
    {
        return this.message;
    }
    
    /**
     * Parses a stack trace string as per the specifications of Mozilla Firefox
     * version 30+.
     * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error/Stack.
     *
     * @param stackTrace - The stack trace string provided by the browser.
     * @return A stack trace that Java can parse.
     */
    private StackTraceElement[] parseFirefoxTrace(String stackTrace)
    {
        StackTraceElement trace;
        String methodName;
        String file;
        String fileLoc;
        int line;
//        int column;
        
        /*
         * Parses the Firefox trace into
         */
        String[] scriptTrace = stackTrace.split("\n");
        StackTraceElement[] javaTrace = new StackTraceElement[scriptTrace.length];
        for (int i = 0, l = scriptTrace.length; i < l; i++)
        {
            methodName = scriptTrace[i].substring(0, scriptTrace[i].indexOf('@'));
            file = scriptTrace[i].substring(methodName.length() + 1, scriptTrace[i].indexOf(':'));
//            column = Integer.parseInt(scriptTrace[i].substring(scriptTrace[i].lastIndexOf(':')));
            /*
             * Windows paths often have colons in their file name, so we can't use the first index of one.
             */
            fileLoc = scriptTrace[i].substring(0, scriptTrace[i].lastIndexOf(':'));
            line = Integer.parseInt(fileLoc.substring(fileLoc.lastIndexOf(':')));
            
            trace = new StackTraceElement(null, null, null, "", methodName, file, line);
            javaTrace[i] = trace;
        }
        
        return javaTrace;
    }
    
    /**
     * Parses a stack trace string as per the specifications of IE 10+ (and used
     * by modern Chromium browsers).
     * https://web.archive.org/web/20140210004225/https://msdn.microsoft.com/en-us/library/windows/apps/hh699850.aspx
     *
     * @param stackTrace - The stack trace string to parse.
     * @return A stack trace that Java can parse.
     */
    private StackTraceElement[] parseChromeTrace(String stackTrace)
    {
        StackTraceElement trace;
        String methodName;
        String fileLoc;
        int line;
//        int column;
        
        /*
         * Parses the Chromium trace.
         */
        String[] scriptTrace = stackTrace.split("\n");
        /*
         * The first line of the trace contains the error message itself. We do
         * not want this in the stack trace.
         */
        StackTraceElement[] javaTrace = new StackTraceElement[scriptTrace.length - 1];
        
        for (int i = 1, l = scriptTrace.length; i < l; i++)
        {
//            column = Integer.parseInt(scriptTrace[i].substring(scriptTrace[i].lastIndexOf(':')));
            fileLoc = scriptTrace[i].substring(0, scriptTrace[i].lastIndexOf(':'));
            line = Integer.parseInt(fileLoc.substring(fileLoc.lastIndexOf(':')));
            methodName = fileLoc.substring(fileLoc.indexOf("at") + 3, fileLoc.lastIndexOf(':'));
            
            trace = new StackTraceElement(null, null, null, "", methodName, null, line);
            javaTrace[i - 1] = trace;
        }
        
        return javaTrace;
    }
}
