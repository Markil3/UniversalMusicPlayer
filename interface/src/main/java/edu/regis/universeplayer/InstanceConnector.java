package edu.regis.universeplayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.Scanner;
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
                service.submit(() -> {
                    try (Scanner scanner = new Scanner(socket.getInputStream()))
                    {
                        String[] args = scanner.nextLine().trim().split(" ");
                        ArrayList<String> parsedArgs = new ArrayList<>();
                        StringBuilder cachedString = null;
                        for (String arg : args)
                        {
                            if (cachedString != null)
                            {
                                if (arg.endsWith("\""))
                                {
                                    cachedString
                                            .append(arg, 0, arg.length() - 1);
                                    parsedArgs.add(cachedString.toString());
                                    cachedString = null;
                                }
                                else
                                {
                                    cachedString.append(arg);
                                }
                            }
                            if (arg.startsWith("\""))
                            {
                                if (arg.endsWith("\""))
                                {
                                    parsedArgs.add(arg.substring(1,
                                            arg.length() - 1));
                                }
                                else
                                {
                                    cachedString =
                                            new StringBuilder(arg.substring(1));
                                }
                            }
                            else
                            {
                                parsedArgs.add(arg);
                            }
                        }
                        args = parsedArgs.toArray(String[]::new);
                        LinkedHashMap<String, Object> ops = new LinkedHashMap<>();
                        parsedArgs.clear();
                        PlayerEnvironment.parseArgs(args, ops, parsedArgs);
                        if (ops.containsKey("h") || ops.containsKey("help"))
                        {
                            PlayerEnvironment.printHelp();
                            return;
                        }
                        PlayerEnvironment.runArguments(ops, parsedArgs,
                                new PrintStream(socket.getOutputStream()));
                        socket.shutdownOutput();
                        /*
                         * Wait for the client to acknowledge the print
                         * before closing the stream.
                         */
                        while (true)
                        {
                            try
                            {
                                scanner.nextInt();
                                break;
                            }
                            catch (InputMismatchException e)
                            {
                            }
                        }
                        logger.debug("Finished request");
                    }
                    catch (IOException e)
                    {
                        logger.error("Error reading socket stream");
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
