package edu.regis.universeplayer.browserCommands;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.net.SocketException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class MessageRunnerTest
{
    /**
     * The input to the runner. This corresponds to the output from the
     * handler.
     *
     * @see #handlerOut
     */
    private static PipedInputStream runnerIn;
    /**
     * The output from the handler and to the runner. This corresponds to the
     * input to the runner.
     *
     * @see #runnerIn
     */
    private static PipedOutputStream handlerOut;
    /**
     * The input to the handler from the runner. This corresponds to the output
     * from the runner.
     *
     * @see #runnerOut
     */
    private static PipedInputStream handlerIn;
    /**
     * The output from the runner and to the handler. This corresponds to the
     * input to the handler.
     *
     * @see #handlerIn
     */
    private static PipedOutputStream runnerOut;
    private static TestRunner runner;
    private static TestHandler handler;
    private static Thread runnerThread;
    private static Thread handlerThread;

    private static class TestRunner extends MessageRunner
    {
        /**
         * Creates a message runner.
         *
         * @param name   - The name of the runner. This is used in logging.
         * @param input  - The input from our external source.
         * @param output - The output to the external source.
         */
        public TestRunner(String name, InputStream input, OutputStream output)
        {
            super(name, input, output);
        }
    }

    private static class TestHandler extends MessageHandler
    {
        /**
         * Creates a message handler.
         *
         * @param name   - The name of the handler. This is used in logging.
         * @param input  - The input from our external source.
         * @param output - The output to the external source.
         */
        public TestHandler(String name, InputStream input, OutputStream output)
        {
            super(name, input, output);
        }
    }

    @BeforeClass
    public static void before()
    {
        try
        {
            runnerIn = new PipedInputStream();
            handlerOut = new PipedOutputStream(runnerIn);
            handlerIn = new PipedInputStream();
            runnerOut = new PipedOutputStream(handlerIn);

            runner = new TestRunner("TestRunner", runnerIn, runnerOut);
            handler = new TestHandler("TestHandler", handlerIn, handlerOut);

            runnerThread = new Thread(runner);
            handlerThread = new Thread(handler);
            runnerThread.start();
            handlerThread.start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Test
    public void testMessage() throws IOException, ExecutionException, InterruptedException
    {
        String mess = "Hello, world!";
        String returnMess = "Goodnight, moon!";
        MessageHandler.MessageListener listener = new MessageHandler.MessageListener()
        {
            @Override
            public Object onMessage(Object providedValue, Object previousReturn) throws IOException, ExecutionException, InterruptedException
            {
                assertEquals(mess, providedValue);
                return returnMess;
            }
        };
        handler.addListener(listener);
        Future<Object> messFuture = runner.sendObject(mess);
        assertEquals(returnMess, messFuture.get());
        handler.removeListener(listener);
    }

    /**
     * Tests messages that arrive in a different order than they were sent.
     *
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testAsyncMessage() throws IOException, ExecutionException, InterruptedException
    {
        final String mess1 = "Hello, world!";
        final String mess2 = "Wake up, sun!";
        final String mess3 = "Good morning, house!";
        final String returnMess1 = "Goodnight, moon!";
        final String returnMess2 = "Good evening, star!";
        final String returnMess3 = "Goodbye, friend!";
        MessageHandler.MessageListener listener = new MessageHandler.MessageListener()
        {
            AtomicBoolean received2 = new AtomicBoolean();

            @Override
            public Object onMessage(Object providedValue, Object previousReturn) throws IOException, ExecutionException, InterruptedException
            {
                assertTrue(providedValue instanceof String);
                switch ((String) providedValue)
                {
                case mess1:
                    synchronized (received2)
                    {
                        while (!received2.get())
                        {
                            received2.wait();
                        }
                    }
                    return returnMess1;
                case mess3:
                    synchronized (received2)
                    {
                        received2.set(true);
                        received2.notifyAll();
                    }
                    return returnMess3;
                case mess2:
                    return returnMess2;
                default:
                    Assert.fail("Expected \"" + mess1 + "\" or \"" + mess1 + "\", actual \"" + providedValue + "\"");
                    return null;
                }
            }
        };
        handler.addListener(listener);
        Future<Object> messFuture = runner.sendObject(mess1);
        Thread.sleep(250);
        assertFalse(messFuture.isDone());
        Future<Object> messFuture2 = runner.sendObject(mess2);
        Thread.sleep(250);
        assertFalse(messFuture.isDone());
        assertTrue(messFuture2.isDone());
        Future<Object> messFuture3 = runner.sendObject(mess3);
        Thread.sleep(250);
        assertTrue(messFuture.isDone());
        assertTrue(messFuture2.isDone());
        assertTrue(messFuture3.isDone());
        assertEquals(returnMess1, messFuture.get());
        assertEquals(returnMess2, messFuture2.get());
        assertEquals(returnMess3, messFuture3.get());
        handler.removeListener(listener);
    }

    /**
     * Tests the handler's update feature.
     *
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testUpdate() throws IOException, ExecutionException, InterruptedException
    {
        final String mess1 = "Hello, world!";
        final AtomicBoolean received = new AtomicBoolean();
        UpdateListener listener = (object, runner) ->
        {
            assertEquals(mess1, object);
            synchronized (received)
            {
                received.set(true);
                received.notifyAll();
            }
        };
        runner.addUpdateListener(listener);
        handler.sendUpdate(mess1);
        synchronized (received)
        {
            while (!received.get())
            {
                received.wait();
            }
        }
        runner.removeUpdateListener(listener);
    }

    @AfterClass
    public static void closeRunner() throws IOException
    {
        runnerOut.close();
        runnerIn.close();
        handler.sendUpdate("ping");
    }
}