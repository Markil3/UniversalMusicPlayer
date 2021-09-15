package edu.regis.universeplayer;

import java.io.Serializable;

/**
 * A test class for pinging the browser.
 */
public class NumberPing implements Serializable
{
    public int number;

    public NumberPing()
    {
        this(0);
    }

    public NumberPing(int number)
    {
        this.number = number;
    }
}
