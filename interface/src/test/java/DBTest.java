/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

import edu.regis.universeplayer.player.Interface;
import org.junit.Test;

import java.io.File;
import java.sql.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class DBTest
{
    @Test
    public void test() throws ClassNotFoundException, SQLException
    {
        try
        {
            Class.forName("org.sqlite.JDBC");
            Connection db;
            Statement stat;
            ResultSet tableResult;
//            db = DriverManager.getConnection("jdbc:sqlite:" + new File(Interface.getDataDir().getAbsolutePath(), "universalmusictest.db").getAbsolutePath());
//            db.setAutoCommit(false);
//            stat = db.createStatement();
//            tableResult = stat.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='INFO';");
//            if (!tableResult.next())
//            {
//                System.out.println("Creating song table");
//                /*
//                 * Create the table
//                 */
//                stat.executeUpdate("CREATE TABLE INFO" +
//                        "(DATA INTEGER PRIMARY KEY NOT NULL);");
//            }
//            else
//            {
//                System.out.println("Clearing table");
//                stat.executeUpdate("DELETE FROM INFO;");
//            }
//            tableResult.close();
//            stat.executeUpdate("INSERT INTO INFO (DATA) VALUES (1);");
//            stat.close();
//            db.commit();
//            db.close();
    
            db = DriverManager.getConnection("jdbc:sqlite:" + new File(Interface.getDataDir().getAbsolutePath(), "universalmusictest.db").getAbsolutePath());
            db.setAutoCommit(false);
            stat = db.createStatement();
            tableResult = stat.executeQuery("SELECT (DATA) FROM INFO");
            assertTrue(tableResult.next());
            assertEquals(1, tableResult.getInt("DATA"));
            tableResult.close();
            stat.close();
            db.close();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            throw e;
        }
    }
}
