package edu.regis.universeplayer.browserCommands;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNull;

public class RunnerCloseTest
{
    private static Logger logger = LoggerFactory.getLogger(RunnerCloseTest.class);
    /**
     * The input to the runner. This corresponds to the output from the
     * handler.
     *
     * @see #handlerOut
     */
    private PipedInputStream runnerIn;
    /**
     * The output from the handler and to the runner. This corresponds to the
     * input to the runner.
     *
     * @see #runnerIn
     */
    private PipedOutputStream handlerOut;
    /**
     * The input to the handler from the runner. This corresponds to the output
     * from the runner.
     *
     * @see #runnerOut
     */
    private PipedInputStream handlerIn;
    /**
     * The output from the runner and to the handler. This corresponds to the
     * input to the handler.
     *
     * @see #handlerIn
     */
    private PipedOutputStream runnerOut;
    private TestRunner runner;
    private TestHandler handler;
    private Thread runnerThread;
    private Thread handlerThread;

    private class TestRunner extends MessageRunner
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

    private class TestHandler extends MessageHandler
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

    @Before
    public void before()
    {
        try
        {
            runnerIn = new PipedInputStream();
            handlerOut = new PipedOutputStream(runnerIn);
            handlerIn = new PipedInputStream();
            runnerOut = new PipedOutputStream(handlerIn);

            runner = new TestRunner("TestRunner", runnerIn, runnerOut);
            handler = new TestHandler("TestHandler", handlerIn, handlerOut);

            handler.addListener((providedValue, previousReturn) -> "ping");

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

    /**
     * Sees what happens when the runner is closed but sends a message anyway.
     *
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testRunnerClose() throws IOException, ExecutionException, InterruptedException
    {
        logger.info("Testing runner close with message");
        runnerIn.close();
        runnerOut.close();
        assertNull(runner.sendObject("pong").get());
        logger.info("Runner test complete");
    }

    /**
     * Sees what happens when the runner is closed and an update is sent from
     * the handler.
     *
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testRunnerCloseUpdate() throws IOException, ExecutionException, InterruptedException
    {
        logger.info("Testing runner close with update");
        runnerIn.close();
        runnerOut.close();
        handler.sendUpdate("ping");
        Thread.sleep(1000);
        logger.info("Runner test complete");
    }

    /**
     * Sees what happens when the handler is closed and the runner attempts to
     * send a message.
     *
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testHandlerClose() throws IOException, ExecutionException, InterruptedException
    {
        logger.info("Testing handler close with message");
        handlerIn.close();
        handlerOut.close();
        assertNull(runner.sendObject("pong").get());
        logger.info("Handler test complete");
    }

    /**
     * Sees what happens when the handler is closed but attempts to send an
     * update anyway.
     *
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testHandlerCloseUpdate() throws IOException, ExecutionException, InterruptedException
    {
        logger.info("Testing handler close with update");
        handlerIn.close();
        handlerOut.close();
        handler.sendUpdate("ping");
        Thread.sleep(1000);
        logger.info("Handler test complete");
    }
}