/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * A default data provider that pulls information from a database.
 */
public abstract class DatabaseProvider<T> implements DataProvider<T>
{
    private static final Logger logger =
            LoggerFactory.getLogger(DatabaseProvider.class);
    private static final ResourceBundle langs = ResourceBundle
            .getBundle("lang.interface", Locale.getDefault());

    protected final ForkJoinPool service = new ForkJoinPool();
    private final LinkedList<UpdateListener> listeners = new LinkedList<>();

    private final AtomicInteger progress = new AtomicInteger(0);
    private final AtomicInteger updating = new AtomicInteger(0);
    private String updateItem;

    private final HashSet<T> collection = new HashSet<>();

    public DatabaseProvider()
    {
        updateCache();
    }

    /**
     * Searches the database and updates the collection from there.
     */
    public final void updateCache()
    {
        updating.set(-1);
        service.submit(new DatabaseProvider.SongQuery(true));
    }

    @Override
    public final void joinUpdate() throws InterruptedException
    {
        synchronized (this.updating)
        {
            if (this.isUpdating())
            {
                this.updating.wait();
            }
        }
    }

    @Override
    public int getUpdateProgress()
    {
        return this.progress.get();
    }

    @Override
    public int getTotalUpdates()
    {
        return this.updating.get();
    }

    @Override
    public String getUpdateText()
    {
        synchronized (this.collection)
        {
            if (this.updateItem != null)
            {
                return new Formatter().format(langs.getString("update" +
                        ".database"), this.updateItem).toString();
            }
            else
            {
                return null;
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that listeners are triggered in the same thread that handles
     * updates, which does block the updates. It is recommended to manually
     * redirect the event to another thread.
     * </p>
     *
     * @param listener - The listener to add.
     */
    @Override
    public void addUpdateListener(UpdateListener listener)
    {
        this.listeners.add(listener);
    }

    @Override
    public void removeUpdateListener(UpdateListener listener)
    {
        this.listeners.remove(listener);
    }

    protected void triggerUpdateListeners()
    {
        this.listeners.forEach(listener -> listener
                .onUpdate(this, this.getUpdateProgress(), this
                        .getTotalUpdates(), this.getUpdateText()));
    }

    /**
     * Obtains the name of
     *
     * @return The name of the database table. This is case insensitive.
     */
    protected abstract String getDatabaseTable();

    /**
     * Obtains the SQL string used to create the database table should it be
     * necessary.
     *
     * @return The SQL command that creates the database table. It should take
     * the format of "CREATE TABLE name (param1 type, param2 type);"
     */
    protected abstract String createDatabaseTable();

    /**
     * Called when an entry is read from the database and is ready to be
     * parsed.
     * <p>
     * Note that this method is called for every row. Do NOT call {@link
     * ResultSet#next()}!
     *
     * @param result The result that is read from.
     */
    protected abstract T readResult(ResultSet result) throws SQLException;

    /**
     * Called to obtain the properties of an object to write.
     *
     * @param item - The item to serialize.
     * @return Properties to write.
     */
    protected abstract Map<String, Object> serializeItem(T item);

    /**
     * Adds an item to the database.
     *
     * @param item - The item to write.
     */
    public final Future<T> writeItem(T item)
    {
        synchronized (this.collection)
        {
//            logger.debug("Writing {}", item);
            this.collection.add(item);
        }
        WriterAction action = new WriterAction(item);
        service.execute(action);
        return action;
    }

    /**
     * Converts a piece of data into a string that will display in the update
     * text.
     *
     * @param data - The data to stringify.
     * @return A string representation of the data being updated.
     */
    protected abstract String stringifyResult(T data);

    /**
     * A callback for when the database scan is complete.
     * <p>
     * Note that this method is called from the same thread that the scanner is
     * from.
     * </p>
     *
     * @return A fork-join task to invoke. This may be null.
     */
    protected abstract ForkJoinTask[] onComplete();

    private void createDatabaseTable(Statement state, String table) throws SQLException
    {
        String rawStatement;
        logger.debug("Creating {} table.", table);
        /*
         * Create the table
         */
        rawStatement = createDatabaseTable();
        if (!Pattern
                .compile("^\\s*CREATE\\s+TABLE\\s*" + table + "\\s*\\(" +
                                "(\\w+\\s+[a-zA-Z0-9()]+(\\s+\\w+)*\\s*)(,\\s*\\w+\\s+[a-zA-Z0-9()]+(\\s+\\w+)*\\s*)*\\);$",
                        Pattern.CASE_INSENSITIVE).matcher(
                        rawStatement).matches())
        {
            throw new IllegalArgumentException("Invalid " +
                    "table creation statement: " +
                    "\"" + rawStatement +
                    "\"");
        }
        state.executeUpdate(rawStatement);
    }

    /**
     * Obtains the collection of items.
     *
     * @return A collection of items parsed from the database.
     */
    @Override
    public final Set<T> getCollection()
    {
        synchronized (this.collection)
        {
            return new HashSet<>(this.collection);
        }
    }

    private class WriterAction extends ForkJoinTask<T>
    {
        private T item;
        private Map<String, Object> string;

        public WriterAction(T item)
        {
            this.item = item;
        }

        /**
         * Returns the result that would be returned by {@link #join}, even if
         * this task completed abnormally, or {@code null} if this task is not
         * known to have been completed.  This method is designed to aid
         * debugging, as well as to support extensions. Its use in any other
         * context is discouraged.
         *
         * @return the result, or {@code null} if not completed
         */
        @Override
        public T getRawResult()
        {
            return item;
        }

        /**
         * Forces the given value to be returned as a result.  This method is
         * designed to support extensions, and should not in general be called
         * otherwise.
         *
         * @param value the value
         */
        @Override
        protected void setRawResult(T value)
        {
            this.item = value;
        }

        /**
         * Immediately performs the base action of this task and returns true
         * if, upon return from this method, this task is guaranteed to have
         * completed. This method may return false otherwise, to indicate that
         * this task is not necessarily complete (or is not known to be
         * complete), for example in asynchronous actions that require explicit
         * invocations of completion methods. This method may also throw an
         * (unchecked) exception to indicate abnormal exit. This method is
         * designed to support extensions, and should not in general be called
         * otherwise.
         *
         * @return {@code true} if this task is known to have completed normally
         */
        @Override
        protected boolean exec()
        {
            ResultSet result;
            Statement state;
            String table = getDatabaseTable();

            this.string = serializeItem(this.item);
            String index =
                    this.string.keySet().stream().findFirst().orElse(null);
            Object indexValue = this.string.get(index);

            synchronized (DatabaseManager.getDb())
            {
                try
                {
                    state = DatabaseManager.getDb().createStatement();
                    result = state
                            .executeQuery("SELECT name FROM sqlite_master" +
                                    " WHERE type='table' AND name='" + table + "';");
                    if (!result.next())
                    {
                        createDatabaseTable(state, table);
                    }
                    PreparedStatement prepState = DatabaseManager.getDb()
                                                                 .prepareStatement(
                                                                         "SELECT * FROM " + table + " WHERE " + index + " " +
                                                                                 "= ?");
                    prepState.setObject(1, indexValue);
                    result = prepState.executeQuery();
                    if (result.next())
                    {
                        logger.debug("Updating {}", this.string);
                        this.string.forEach((key, value) -> {
                            try
                            {
                                PreparedStatement prepState1 =
                                        DatabaseManager.getDb()
                                                       .prepareStatement(
                                                               "UPDATE " + table + " SET " + key +
                                                                       " = ? WHERE " +
                                                                       index + " = ?");
                                prepState1.setObject(1, indexValue);
                                prepState1.setObject(2, value);
                            }
                            catch (SQLException throwables)
                            {
                                logger.error("Could not update " + key, throwables);
                            }
                        });
                    }
                    else
                    {
                        logger.debug("Inserting {}", this.string);
                        AtomicInteger i = new AtomicInteger(1);
                        PreparedStatement finalPrepState = DatabaseManager
                                .getDb().prepareStatement(
                                        "INSERT INTO " + table + " VALUES (?" + ", ?"
                                                .repeat(this.string
                                                        .size() - 1) +
                                                ")");
                        this.string.forEach((key, value) -> {
                            try
                            {
                                finalPrepState
                                        .setObject(i.getAndIncrement(), value);
                            }
                            catch (SQLException throwables)
                            {
                                logger.error("Could not update " + key, throwables);
                            }
                        });
                        int count = finalPrepState.executeUpdate();
                        if (count == 0)
                        {
                            logger.error("Failed to insert {}", this.string);
                        }
                    }
                }
                catch (Exception e)
                {
                    logger.error("Could not write object {}", this.string, e);
                }
            }

            return true;
        }
    }

    /**
     * Scans the database for information
     */
    private class SongQuery extends ForkJoinTask<Void>
    {
        private final boolean scan;

        SongQuery(boolean scan)
        {
            this.scan = scan;
        }

        @Override
        public Void getRawResult()
        {
            return null;
        }

        @Override
        protected void setRawResult(Void value)
        {

        }

        @Override
        protected boolean exec()
        {
            Statement state;
            String table = getDatabaseTable();
            ResultSet result;
            T item;

            synchronized (updating)
            {
                updating.set(-1);
                try
                {
                    logger.debug("Querying database.");
                    /*
                     * Check if the table exists
                     */
                    synchronized (DatabaseManager.getDb())
                    {
                        state = DatabaseManager.getDb().createStatement();
                        result = state
                                .executeQuery("SELECT name FROM sqlite_master" +
                                        " WHERE type='table' AND name='" + table + "';");
                        if (!result.next())
                        {
                            createDatabaseTable(state, table);
                        }
                        else
                        {
                            result = state
                                    .executeQuery("SELECT count(*) FROM " + table + ";");
                            updating.set(result.getInt(1));
                            triggerUpdateListeners();
                            result = state
                                    .executeQuery("SELECT * FROM " + table + ";");

                            while (result.next())
                            {
                                item = readResult(result);
                                synchronized (collection)
                                {
                                    updateItem = stringifyResult(item);
                                    collection.add(item);
                                    progress.incrementAndGet();
                                }
                                triggerUpdateListeners();
                            }
                        }
                        state.close();
                    }
                }
                catch (SQLException e)
                {
                    logger.error("Could not query SQL database.", e);
                }
                finally
                {
                    logger.debug("Query complete, retrieved {} items",
                            collection.size());

                    progress.set(0);
                    updating.set(0);
                    updateItem = null;
                    updating.notifyAll();
                    triggerUpdateListeners();
                    logger.debug("Searching for post-query tasks");
                    try
                    {
                        ForkJoinTask[] runners = onComplete();
                        if (runners != null)
                        {
                            logger.debug("Running {} post-query tasks", runners.length);
                            invokeAll(runners);
                        }
                    }
                    catch (Exception e)
                    {
                        logger.error("Could not get runners", e);
                    }
                }
            }
            return true;
        }
    }
}
