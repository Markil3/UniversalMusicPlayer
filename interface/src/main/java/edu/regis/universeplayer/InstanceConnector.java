package edu.regis.universeplayer;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This runner is used to handle other instances of the program wanting to
 * manipulate the main state.
 *
 * @author William Hubbard.
 * @version 0.1
 */
public class InstanceConnector implements Runnable
{
    private static final Logger logger =
            LoggerFactory.getLogger(InstanceConnector.class);
    private AtomicBoolean running = new AtomicBoolean(true);
    ExecutorService service = Executors.newFixedThreadPool(5);

    @Override
    public void run()
    {
        ServerSocket server = null;
        try
        {
            server = new ServerSocket(ConfigManager.PORT, 50, InetAddress
                    .getByName(null));

            while (this.running.get())
            {
                Socket socket = server.accept();
                service.submit(() ->
                {
                    try
                    {
                        ObjectInputStream scanner = new ObjectInputStream(socket.getInputStream());
                        PrintStream stream = new PrintStream(socket.getOutputStream());
                        CommandLine cmd = (CommandLine) scanner.readObject();
                        logger.debug("Receiving commands: " + Arrays.toString(cmd.getArgs()));
                        if (cmd.hasOption("help"))
                        {
                            PlayerEnvironment.printHelp(stream);
                        }
                        else
                        {
                            PlayerEnvironment.runArguments(cmd,
                                    stream);
                        }
                        /*
                         * Wait for the client to acknowledge the print
                         * before closing the stream.
                         */
                        stream.println();
                        stream.println("END");
                        scanner.readInt();
                        logger.debug("Finished request");
                    }
                    catch (EOFException e)
                    {
                        /*
                         * Doesn't really matter
                         */
                    }
                    catch (IOException | ClassNotFoundException e)
                    {
                        logger.error("Error reading socket stream", e);
                    }
                    finally
                    {
                        try
                        {
                            socket.close();
                        }
                        catch (IOException e)
                        {
                            logger.error("Could not close socket", e);
                        }
                    }
                });
            }
        }
        catch (IOException e)
        {
            logger.error("Error starting server", e);
        }
        finally
        {
            if (server != null)
            {
                try
                {
                    server.close();
                }
                catch (IOException e)
                {
                    logger.error("Unable to close server", e);
                }
            }
        }
    }

    /**
     * Stops the server.
     */
    public void stop()
    {
        this.running.set(false);
    }
}
