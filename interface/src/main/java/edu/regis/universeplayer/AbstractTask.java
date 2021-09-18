package edu.regis.universeplayer;

import java.util.concurrent.ForkJoinTask;

/**
 * An abstract {@link ForkJoinTask} that has getters and setters automatically
 * set up.
 *
 * @param <T>
 */
public abstract class AbstractTask<T> extends ForkJoinTask<T>
{
    private T value;

    /**
     * Returns the result that would be returned by {@link #join}, even if this
     * task completed abnormally, or {@code null} if this task is not known to
     * have been completed.  This method is designed to aid debugging, as well
     * as to support extensions. Its use in any other context is discouraged.
     *
     * @return the result, or {@code null} if not completed
     */
    @Override
    public T getRawResult()
    {
        return this.value;
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
        this.value = value;
    }
}
