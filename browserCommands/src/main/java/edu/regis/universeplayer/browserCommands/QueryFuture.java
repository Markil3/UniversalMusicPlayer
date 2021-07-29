/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.browserCommands;

import java.util.concurrent.*;

/**
 * This future contains extra information regarding the success of a browser
 * command on the browser side.
 *
 * @param <V> The return type.
 */
public interface QueryFuture<V> extends Future<V>
{
    /**
     * Waits if necessary for the computation to complete, and then checks to see whether execution was successful.
     *
     * @return A status code on whether the command was successful or not.
     * @throws CancellationException – if the computation was cancelled
     * @throws ExecutionException    – if the computation threw an exception
     * @throws InterruptedException  – if the current thread was interrupted while waiting
     */
    CommandConfirmation getConfirmation() throws CancellationException, ExecutionException, InterruptedException;
    
    /**
     * Waits if necessary for the computation to complete, and then checks to see whether execution was successful.
     *
     * @param timeout - How long to hang the thread.
     * @param unit    - What units was specified in the timeout parameter.
     * @return A status code on whether the command was successful or not.
     * @throws TimeoutException     – if the computation timed out
     * @throws ExecutionException   – if the computation threw an exception
     * @throws InterruptedException – if the current thread was interrupted while waiting
     */
    CommandConfirmation getConfirmation(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
