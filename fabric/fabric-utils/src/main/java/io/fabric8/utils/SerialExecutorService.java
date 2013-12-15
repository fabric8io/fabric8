/**
 * Copyright (C) Red Hat, Inc.
 * http://redhat.com
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
package io.fabric8.utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An ExecutorService which ensures serial execution of the Runnable
 * objects which it is asked to execute.  By default it delegates
 * execution of those tasks to a thread pool, but can be configured
 * to use any Executor.
 */
public class SerialExecutorService extends AbstractExecutorService {

    static long THREAD_POOL_KEEP_ALIVE = Integer.getInteger("io.fabric8.utils.THREAD_POOL_KEEP_ALIVE", 5000);
    static final ThreadGroup group = new ThreadGroup("Fabric Tasks");

    static final Executor threadPool = new Executor() {
        SynchronousQueue<Runnable> queue = new SynchronousQueue<Runnable>();

        @Override
        public void execute(final Runnable task) {

            if (task == null) {
                throw new NullPointerException();
            }

            // Lets try to give the task to a running thread..
            if (!queue.offer(task)) {

                // Existing thread did not take the task, so spin
                // up a thread to execute the task..
                new Thread(group, "Fabric Task") {
                    @Override
                    public void run() {
                        while (true) {
                            Runnable task;
                            try {
                                task = queue.poll(THREAD_POOL_KEEP_ALIVE, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e) {
                                return;
                            }
                            if (task == null) {
                                return;
                            }
                            task.run();
                        }
                    }
                }.start();

                // Now wait till a thread picks it up..
                try {
                    queue.put(task);
                } catch (InterruptedException e) {
                    throw new RejectedExecutionException(e);
                }
            }
        }
    };

    protected Executor target;
    protected volatile String label;
    protected AtomicBoolean shutdown = new AtomicBoolean(false);
    protected AtomicBoolean terminated = new AtomicBoolean(false);
    protected CountDownLatch terminatedLatch = new CountDownLatch(1);
    protected final AtomicBoolean triggered = new AtomicBoolean();
    protected final ConcurrentLinkedQueue<Runnable> externalQueue = new ConcurrentLinkedQueue<Runnable>();
    protected final LinkedList<Runnable> localQueue = new LinkedList<Runnable>();
    protected final ThreadLocal<Boolean> draining = new ThreadLocal<Boolean>();
    protected final Runnable drainTask = new Runnable() {
        public void run() {
            drain();
        }
    };


    public SerialExecutorService() {
        this("<no-label>");
    }

    public SerialExecutorService(String label) {
        this(threadPool, label);
    }

    public SerialExecutorService(Executor target) {
        this(target, "<no-label>");
    }

    public SerialExecutorService(Executor target, String label) {
        this.target = target;
        this.label = label;
    }

    /**
     * Queues the runnable for execution.
     * @param runnable
     */
    @Override
    public void execute(Runnable runnable) {
        if (runnable == null)
            throw new NullPointerException("runnable cannot be null");
        if (shutdown.get())
            throw new RejectedExecutionException("shutdown");

        if (isDraining()) {
            localQueue.add(runnable);
        } else {
            externalQueue.add(runnable);
            triggerDrain();
        }
    }

    /**
     * Executes the runnable.  This method blocks until it has been
     * executed.
     * @param runnable
     */
    public void executeAndDrain(Runnable runnable) {
        if (runnable == null)
            throw new NullPointerException("runnable cannot be null");
        if (shutdown.get())
            throw new RejectedExecutionException("shutdown");

        if (isDraining()) {
            runnable.run();
        } else {
            externalQueue.add(runnable);
            drain();
        }
    }

    protected void triggerDrain() {
        if (triggered.compareAndSet(false, true)) {
            target.execute(drainTask);
        }
    }

    /**
     * This method blocks until all previously queued Runnable objects are run.
     */
    synchronized public void drain() {
        draining.set(Boolean.TRUE);
        try {
            boolean drained = false;
            while (!drained) {
                Runnable runnable = localQueue.poll();
                if (runnable == null) {
                    runnable = externalQueue.poll();
                }
                if (runnable == null) {
                    drained = true;
                } else {
                    try {
                        runnable.run();
                    } catch (Throwable e) {
                        Thread thread = Thread.currentThread();
                        thread.getUncaughtExceptionHandler().uncaughtException(thread, e);
                    }
                }
            }
        } finally {
            draining.remove();
            triggered.set(false);
            if (!externalQueue.isEmpty()) {
                triggerDrain();
            }
        }
    }


    @Override
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            externalQueue.add(new Runnable() {
                @Override
                public void run() {
                    terminated.set(true);
                    terminatedLatch.countDown();
                }
            });
            triggerDrain();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.EMPTY_LIST;
    }

    public boolean isDraining() {
        return draining.get() == Boolean.TRUE;
    }


    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public boolean isTerminated() {
        return terminated.get();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return terminatedLatch.await(timeout, unit);
    }

    @Override
    public String toString() {
        return label;
    }

    public Executor getTarget() {
        return target;
    }

    public void setTarget(Executor target) {
        this.target = target;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
