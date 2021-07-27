/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.addon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Main
{
    private static Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) throws ExecutionException, InterruptedException
    {
        LinkedList<Future<Object>> requests = new LinkedList<>();
        BrowserLink link = new BrowserLink();
        Thread linkThread = new Thread(link);
        linkThread.start();
        logger.info("Sending message");
        for (int i = 0, l = 20; i < l; i++)
        {
            Future<Object> future = link.sendObject("ping");
            requests.add(future);
        }
        while (requests.size() > 0)
        {
            logger.info("Message received: {}", requests.poll().get());
        }
    }
}
