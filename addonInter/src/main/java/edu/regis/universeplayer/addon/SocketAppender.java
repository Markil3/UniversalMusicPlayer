package edu.regis.universeplayer.addon;

import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This appender will store all messages sent to it, for the interface link to
 * later collect and forward to the interface.
 *
 * @author William Hubbard
 */
@Plugin(
        name = "Socket",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE)
public class SocketAppender extends AbstractAppender
{
    private static final LinkedList<LogEvent> events = new LinkedList<>();

    /**
     * Builds FileAppender instances.
     *
     * @param <B> The type to build
     */
    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<SocketAppender>
    {
        @Override
        public SocketAppender build()
        {
            return new SocketAppender(getName(), getFilter(), getOrCreateLayout(), isIgnoreExceptions(), getPropertyArray());
        }
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder()
    {
        return new Builder<B>().asBuilder();
    }

    public SocketAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
                          final boolean ignoreExceptions, final Property[] properties)
    {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    /**
     * Logs a LogEvent using whatever logic this Appender wishes to use. It is
     * typically recommended to use a bridge pattern not only for the benefits
     * from decoupling an Appender from its implementation, but it is also handy
     * for sharing resources which may require some form of locking.
     *
     * @param event The LogEvent.
     */
    @Override
    public void append(LogEvent event)
    {
        synchronized (events)
        {
            events.add(event);
        }
    }

    /**
     * Checks to see if any logs are available.
     * @return
     */
    public static boolean hasLogs()
    {
        synchronized (events)
        {
            return !events.isEmpty();
        }
    }

    public static List<LogEvent> retrieveLogEvents()
    {
        synchronized (events)
        {
            List<LogEvent> logs = events.stream().toList();
            events.clear();
            return logs;
        }
    }
}
