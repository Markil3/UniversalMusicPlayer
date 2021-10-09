package edu.regis.universeplayer;

import java.io.Serializable;
import java.net.URL;

/**
 * A test class for pinging the browser.
 */
public class NumberPing implements Serializable
{
    private final URL url;
    public double number;

    public NumberPing()
    {
        this(0);
    }

    public NumberPing(double number)
    {
        this.url = null;
        this.number = number;
    }

    public NumberPing(URL url, double number)
    {
        this.url = url;
        this.number = number;
    }
}
