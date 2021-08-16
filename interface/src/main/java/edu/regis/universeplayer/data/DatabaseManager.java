/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import edu.regis.universeplayer.player.Interface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLWarning;

/**
 * Manages the application song database
 */
public class DatabaseManager
{
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static Connection db;
    
    public static synchronized Connection getDb()
    {
        SQLWarning warning;
        try
        {
            if (db == null || db.isClosed())
            {
                Class.forName("org.sqlite.JDBC");
                db = DriverManager.getConnection("jdbc:sqlite:" + new File(Interface.getDataDir().getAbsolutePath(), "universalmusic.db").getAbsolutePath());
            }
            warning = db.getWarnings();
            while (warning != null)
            {
                logger.warn("SQL Warning: ", warning);
                warning = warning.getNextWarning();
            }
            db.clearWarnings();
        }
        catch (SQLException | ClassNotFoundException e)
        {
            logger.error("Could not store caching DIR.");
        }
        return db;
    }
}
