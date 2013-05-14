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
package org.fusesource.fabric.internal.locks;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.fusesource.fabric.api.Lock;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.createDefault;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.exists;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.delete;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;

public class LockImpl implements Lock {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockImpl.class);

    private final String path;
    private final IZKClient zooKeeper;
    private final ConcurrentMap<Thread, LockData> threadLocks = new ConcurrentHashMap<Thread, LockData>();

    private final Watcher watcher = new Watcher() {
        @Override
        public void process(WatchedEvent event) {
            doNotifyAll();
        }
    };

    /**
     * Constructor
     *
     * @param zooKeeper
     * @param path
     */
    protected LockImpl(IZKClient zooKeeper, String path) {
        this.path = path;
        this.zooKeeper = zooKeeper;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        long waitFor = unit.toMillis(time);
        long start = System.currentTimeMillis();
        LockData lockData;
        String lockPath = null;
        try {
            createDefault(zooKeeper, path, "");
            Thread current = Thread.currentThread();

            lockData = threadLocks.get(current);

            if (lockData == null) {
                lockPath = createLockNode(path);
                setData(zooKeeper, lockPath, System.getProperty("karaf.name") + "/" + current.getName());
                lockData = new LockData(current, lockPath);
                threadLocks.put(current, lockData);
            } else {
                lockData.getCount().incrementAndGet();
                return true;
            }

            while (start + waitFor >= System.currentTimeMillis()) {
                List<String> children = zooKeeper.getChildren(path, watcher);
                String id = stripPath(lockData.getLockPath());
                if (hasLock(id, children)) {
                    return true;
                } else {
                    synchronized (this) {
                        wait(start + waitFor - System.currentTimeMillis());
                    }
                }
            }
            threadLocks.remove(current);
            deleteLockNode(lockPath);
            return false;
        } catch (Exception ex) {
            LOGGER.warn("Error while trying to acquire lock on: {}.", path, ex);
            deleteLockNode(lockPath);
            return false;
        }

    }

    @Override
    public void unlock() {
        try {
            Thread current = Thread.currentThread();
            LockData lockData = threadLocks.get(current);
            if (lockData != null && lockData.getCount().decrementAndGet() <= 0) {
                threadLocks.remove(current);
                if (exists(zooKeeper, lockData.getLockPath()) != null) {
                    delete(zooKeeper, lockData.getLockPath());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String stripPath(String node) {
        if (node.contains("/")) {
            return node.substring(node.lastIndexOf("/") + 1);
        } else {
            return node;
        }
    }

    private String createLockNode(String path) {
        try {
            return zooKeeper.create(ZkPath.LOCK.getPath(path), CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteLockNode(String path) {
        try {
            delete(zooKeeper, path);
        } catch (Exception e) {
            //ignore
        }
    }

    /**
     * Returns true if the specified id is the lowest from all candidates.
     *
     * @param id
     * @param allCandidates
     * @return
     */
    private static boolean hasLock(String id, List<String> allCandidates) {
        if (allCandidates == null || allCandidates.size() == 1) {
            return true;
        }
        long our = Long.parseLong(id);
        for (String child : allCandidates) {
            long their = Long.parseLong(child);
            if (their < our) {
                return false;
            }
        }
        return true;
    }

    /**
     * Notifies all.
     */
    private synchronized void doNotifyAll() {
        notifyAll();
    }
}
