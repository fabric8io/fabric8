/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.download;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A default implementation of {@link Future}.
 */
public class DefaultFuture<T extends Future> implements Future<T> {

    /**
     * A default value to indicate the future has been canceled
     */
    private static final Object CANCELED = new Object();

    /**
     * A number of seconds to wait between two deadlock controls ( 5 seconds )
     */
    private static final long DEAD_LOCK_CHECK_INTERVAL = 5000L;

    /**
     * A lock used by the wait() method
     */
    private final Object lock;
    private FutureListener<T> firstListener;
    private List<FutureListener<T>> otherListeners;
    private Object result;
    private boolean ready;
    private int waiters;


    public DefaultFuture() {
        this(null);
    }

    /**
     * Creates a new instance.
     */
    public DefaultFuture(Object lock) {
        this.lock = lock != null ? lock : this;
    }

    /**
     * {@inheritDoc}
     */
    public T await() throws InterruptedException {
        synchronized (lock) {
            while (!ready) {
                waiters++;
                try {
                    // Wait for a notify, or if no notify is called,
                    // assume that we have a deadlock and exit the
                    // loop to check for a potential deadlock.
                    lock.wait(DEAD_LOCK_CHECK_INTERVAL);
                } finally {
                    waiters--;
                    if (!ready) {
                        checkDeadLock();
                    }
                }
            }
        }
        return (T) this;
    }

    /**
     * {@inheritDoc}
     */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return await(unit.toMillis(timeout));
    }

    /**
     * {@inheritDoc}
     */
    public boolean await(long timeoutMillis) throws InterruptedException {
        return await0(timeoutMillis, true);
    }

    /**
     * {@inheritDoc}
     */
    public T awaitUninterruptibly() {
        try {
            await0(Long.MAX_VALUE, false);
        } catch (InterruptedException ie) {
            // Do nothing : this catch is just mandatory by contract
        }

        return (T) this;
    }

    /**
     * {@inheritDoc}
     */
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return awaitUninterruptibly(unit.toMillis(timeout));
    }

    /**
     * {@inheritDoc}
     */
    public boolean awaitUninterruptibly(long timeoutMillis) {
        try {
            return await0(timeoutMillis, false);
        } catch (InterruptedException e) {
            throw new InternalError();
        }
    }

    /**
     * Wait for the Future to be ready. If the requested delay is 0 or
     * negative, this method immediately returns the value of the
     * 'ready' flag.
     * Every 5 second, the wait will be suspended to be able to check if
     * there is a deadlock or not.
     *
     * @param timeoutMillis The delay we will wait for the Future to be ready
     * @param interruptable Tells if the wait can be interrupted or not
     * @return <code>true</code> if the Future is ready
     * @throws InterruptedException If the thread has been interrupted
     *                              when it's not allowed.
     */
    private boolean await0(long timeoutMillis, boolean interruptable) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeoutMillis;

        synchronized (lock) {
            if (ready) {
                return ready;
            } else if (timeoutMillis <= 0) {
                return ready;
            }

            waiters++;
            try {
                for (; ;) {
                    try {
                        long timeOut = Math.min(timeoutMillis, DEAD_LOCK_CHECK_INTERVAL);
                        lock.wait(timeOut);
                    } catch (InterruptedException e) {
                        if (interruptable) {
                            throw e;
                        }
                    }

                    if (ready) {
                        return true;
                    } else {
                        if (endTime < System.currentTimeMillis()) {
                            return ready;
                        }
                    }
                }
            } finally {
                waiters--;
                if (!ready) {
                    checkDeadLock();
                }
            }
        }
    }


    /**
     * TODO checkDeadLock.
     */
    private void checkDeadLock() {
//        // Only read / write / connect / write future can cause dead lock.
//        if (!(this instanceof CloseFuture || this instanceof WriteFuture ||
//              this instanceof ReadFuture || this instanceof ConnectFuture)) {
//            return;
//        }
//
//        // Get the current thread stackTrace.
//        // Using Thread.currentThread().getStackTrace() is the best solution,
//        // even if slightly less efficient than doing a new Exception().getStackTrace(),
//        // as internally, it does exactly the same thing. The advantage of using
//        // this solution is that we may benefit some improvement with some
//        // future versions of Java.
//        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
//
//        // Simple and quick check.
//        for (StackTraceElement s: stackTrace) {
//            if (AbstractPollingIoProcessor.class.getName().equals(s.getClassName())) {
//                IllegalStateException e = new IllegalStateException( "t" );
//                e.getStackTrace();
//                throw new IllegalStateException(
//                    "DEAD LOCK: " + IoFuture.class.getSimpleName() +
//                    ".await() was invoked from an I/O processor thread.  " +
//                    "Please use " + IoFutureListener.class.getSimpleName() +
//                    " or configure a proper thread model alternatively.");
//            }
//        }
//
//        // And then more precisely.
//        for (StackTraceElement s: stackTrace) {
//            try {
//                Class<?> cls = DefaultSshFuture.class.getClassLoader().loadClass(s.getClassName());
//                if (IoProcessor.class.isAssignableFrom(cls)) {
//                    throw new IllegalStateException(
//                        "DEAD LOCK: " + IoFuture.class.getSimpleName() +
//                        ".await() was invoked from an I/O processor thread.  " +
//                        "Please use " + IoFutureListener.class.getSimpleName() +
//                        " or configure a proper thread model alternatively.");
//                }
//            } catch (Exception cnfe) {
//                // Ignore
//            }
//        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDone() {
        synchronized (lock) {
            return ready;
        }
    }

    /**
     * Sets the result of the asynchronous operation, and mark it as finished.
     */
    public void setValue(Object newValue) {
        synchronized (lock) {
            // Allow only once.
            if (ready) {
                return;
            }

            result = newValue;
            ready = true;
            if (waiters > 0) {
                lock.notifyAll();
            }
        }

        notifyListeners();
    }

    /**
     * Returns the result of the asynchronous operation.
     */
    protected Object getValue() {
        synchronized (lock) {
            return result;
        }
    }

    /**
     * {@inheritDoc}
     */
    public T addListener(FutureListener<T> listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        boolean notifyNow = false;
        synchronized (lock) {
            if (ready) {
                notifyNow = true;
            } else {
                if (firstListener == null) {
                    firstListener = listener;
                } else {
                    if (otherListeners == null) {
                        otherListeners = new ArrayList<FutureListener<T>>(1);
                    }
                    otherListeners.add(listener);
                }
            }
        }

        if (notifyNow) {
            notifyListener(listener);
        }
        return (T) this;
    }

    /**
     * {@inheritDoc}
     */
    public T removeListener(FutureListener<T> listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        synchronized (lock) {
            if (!ready) {
                if (listener == firstListener) {
                    if (otherListeners != null && !otherListeners.isEmpty()) {
                        firstListener = otherListeners.remove(0);
                    } else {
                        firstListener = null;
                    }
                } else if (otherListeners != null) {
                    otherListeners.remove(listener);
                }
            }
        }

        return (T) this;
    }

    private void notifyListeners() {
        // There won't be any visibility problem or concurrent modification
        // because 'ready' flag will be checked against both addListener and
        // removeListener calls.
        if (firstListener != null) {
            notifyListener(firstListener);
            firstListener = null;

            if (otherListeners != null) {
                for (FutureListener<T> l : otherListeners) {
                    notifyListener(l);
                }
                otherListeners = null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void notifyListener(FutureListener<T> l) {
        try {
            l.operationComplete((T) this);
        } catch (Throwable t) {
            // TODO
            t.printStackTrace();
        }
    }

    public boolean isCanceled() {
        return getValue() == CANCELED;
    }

    public void cancel() {
        setValue(CANCELED);
    }
}
