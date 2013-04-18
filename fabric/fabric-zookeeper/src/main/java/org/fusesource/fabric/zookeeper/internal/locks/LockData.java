package org.fusesource.fabric.zookeeper.internal.locks;

import java.util.concurrent.atomic.AtomicInteger;

public class LockData {

    private final Thread thread;
    private final String lockPath;
    private final AtomicInteger count = new AtomicInteger();

    public LockData(Thread thread, String lockPath) {
        this.thread = thread;
        this.lockPath = lockPath;
    }

    public String getLockPath() {
        return lockPath;
    }

    public AtomicInteger getCount() {
        return count;
    }
}
