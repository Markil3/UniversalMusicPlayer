package edu.regis.universeplayer;

import java.io.Serializable;
import java.util.Arrays;

public class Log implements Serializable
{
    public String logger;
    public String level;
    public Object[] message;

    @Override
    public String toString()
    {
        return "LOG " + this.level.toUpperCase() + ": " +
                Arrays.toString(message);
    }
}
