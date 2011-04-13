/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.agent.download;

import java.util.concurrent.TimeUnit;

/**
 * Represents the completion of an asynchronous download operation on a given object.
 * Can be listened for completion using a {@link FutureListener}.
 */
public interface Future<T extends Future> {

    /**
     * Wait for the asynchronous operation to complete.
     * The attached listeners will be notified when the operation is
     * completed.
     */
    T await() throws InterruptedException;

    /**
     * Wait for the asynchronous operation to complete with the specified timeout.
     *
     * @return <tt>true</tt> if the operation is completed.
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Wait for the asynchronous operation to complete with the specified timeout.
     *
     * @return <tt>true</tt> if the operation is completed.
     */
    boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * Wait for the asynchronous operation to complete uninterruptibly.
     * The attached listeners will be notified when the operation is
     * completed.
     *
     * @return the current IoFuture
     */
    T awaitUninterruptibly();

    /**
     * Wait for the asynchronous operation to complete with the specified timeout
     * uninterruptibly.
     *
     * @return <tt>true</tt> if the operation is completed.
     */
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    /**
     * Wait for the asynchronous operation to complete with the specified timeout
     * uninterruptibly.
     *
     * @return <tt>true</tt> if the operation is finished.
     */
    boolean awaitUninterruptibly(long timeoutMillis);

    /**
     * Returns if the asynchronous operation is completed.
     */
    boolean isDone();

    /**
     * Adds an event <tt>listener</tt> which is notified when
     * this future is completed. If the listener is added
     * after the completion, the listener is directly notified.
     */
    T addListener(FutureListener<T> listener);

    /**
     * Removes an existing event <tt>listener</tt> so it won't be notified when
     * the future is completed.
     */
    T removeListener(FutureListener<T> listener);

}
