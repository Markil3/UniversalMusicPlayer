/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.addon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main
{
    private static Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args)
    {
        boolean running = true;
        logger.info("This is an INFO level log message!");
        logger.error("This is an ERROR level log message!");
        logger.warn("This could be problimatic");
        logger.info("https://sematext.com/blog/log4j2-tutorial/#toc-log4j-2-configuration-1");
        while (running)
        {
            logger.info("Waiting1");
            logger.debug("Waiting");
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                logger.trace("Something interrupted us", e);
                running = false;
            }
        }
    }
}
