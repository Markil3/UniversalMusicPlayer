import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import edu.regis.universeplayer.NumberPing;
import edu.regis.universeplayer.browser.Browser;
import edu.regis.universeplayer.browserCommands.CommandReturn;

public class PingTest
{
    @BeforeClass
    public static void setupBrowser() throws IOException, InterruptedException
    {
        System.out.println("Creating browser");
        Browser.createBrowser();
        System.out.println("Browser created");
        if (Browser.getInstance() == null)
        {
            Browser.waitInstance();
        }
        Thread thread = new Thread(Browser.getInstance());
        thread.start();
        System.out.println("Browser fully initialized");
    }

    @AfterClass
    public static void closeBrowser() throws IOException, InterruptedException
    {
        Browser.getInstance().stop();
        System.out.println("Browser closed");
    }

    @Test
    public void testPing() throws IOException, InterruptedException, ExecutionException
    {
        ArrayList<Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++)
        {
            futures.add(Browser.getInstance().sendObject("ping"));
        }
        System.out.println("Pings sent");
        for (int i = 0; i < futures.size(); i++)
        {
            assertEquals("pong",
                    ((CommandReturn<String>) futures.get(i).get())
                            .getReturnValue());
        }
        System.out.println("Objects received");
    }

    @Test
    public void testNumPing() throws IOException, InterruptedException,
            ExecutionException
    {
        ArrayList<Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++)
        {
            futures.add(Browser.getInstance().sendObject(new NumberPing(i)));
        }
        System.out.println("Numbered pings sent");
        for (int i = 0; i < futures.size(); i++)
        {
            assertEquals(i,
                    ((CommandReturn<Double>) futures.get(i).get())
                            .getReturnValue(), 0.001);
        }
        System.out.println("Objects received");
    }
}
