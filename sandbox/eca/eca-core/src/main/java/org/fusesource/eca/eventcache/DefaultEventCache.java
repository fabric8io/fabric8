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

package org.fusesource.eca.eventcache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fusesource.eca.util.LinkedNode;
import org.fusesource.eca.util.ParsingUtil;

/**
 * A serial cache of Exchanges
 */
public class DefaultEventCache<T> implements EventCache<T> {
    static final int NOT_SET = -1;
    private final Object id;
    private int windowCount = NOT_SET;
    private long windowTime = NOT_SET;
    //private final List<CacheItem<T>> cache = new LinkedList<CacheItem<T>>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private CacheItemImpl root = null;
    private CacheItemImpl tail = null;
    private int size = 0;
    private EventClock eventClock = new SystemEventClock();
    private final Set<T> set = new HashSet<T>();

    public DefaultEventCache(Object id, String size) {
        this.id = id;
        setWindow(size);
    }

    protected DefaultEventCache() {
        this.id = "";
    }

    public boolean add(T item) {
        if (set.add(item)) {
            CacheItemImpl node = new CacheItemImpl();
            node.item = item;
            long timestamp = NOT_SET;
            if (windowTime > 0) {
                timestamp = getEventClock().currentTimeMillis();
                node.timeStamp = timestamp;
            }
            try {
                lock.writeLock().lock();
                if (root == null) {
                    root = node;
                } else {
                    tail.linkAfter(node);
                }
                tail = node;
                size++;
            } finally {
                lock.writeLock().unlock();
            }
            pruneCache(timestamp);
            return true;
        }
        return false;
    }


    public List<T> getWindow() {
        pruneCache(getEventClock().currentTimeMillis());
        List<T> result = new ArrayList<T>(size);
        try {

            lock.readLock().lock();


            CacheItemImpl node = root;
            while (node != null) {
                result.add(node.item);
                node = node.getNext();
            }
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    public List<CacheItem<T>> getCacheItems() {
        pruneCache(getEventClock().currentTimeMillis());
        List<CacheItem<T>> result = new ArrayList<CacheItem<T>>(size);
        try {
            lock.readLock().lock();
            CacheItemImpl node = root;
            while (node != null) {
                result.add(node);
                node = node.getNext();
            }
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    public void setWindow(String text) throws IllegalArgumentException {
        if (text != null) {
            String[] split = text.split(",");
            for (int i = 0; i < split.length; i++) {
                setWindowElement(split[i].trim());
            }
        }
    }

    protected void setWindowElement(String text) throws IllegalArgumentException {
        Pattern p = Pattern.compile("^\\s*(\\d+)\\s*(b)?\\s*$", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.matches()) {
            windowCount = Integer.valueOf(Integer.parseInt(m.group(1)));
            return;
        }
        windowTime = ParsingUtil.getTimeAsMilliseconds(text);
        if (windowTime <= 0) {
            throw new IllegalArgumentException("Could not convert to a window size: " + text);
        }
    }

    public int getWindowCount() {
        return windowCount;
    }

    public void setWindowCount(int windowCount) {
        this.windowCount = windowCount;
    }

    public long getWindowTime() {
        return windowTime;
    }

    public void setWindowTime(long windowTime) {
        this.windowTime = windowTime;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        pruneCache(eventClock.currentTimeMillis());
        try {
            lock.readLock().lock();
            return size;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        try {
            lock.writeLock().lock();
            root = null;
            tail = null;
            set.clear();
            size = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @return the EventClock being used
     */
    public EventClock getEventClock() {
        return this.eventClock;
    }

    /**
     * Set the EventClock to use
     */
    public void setEventClock(EventClock eventClock) {
        this.eventClock = eventClock;
    }

    protected void pruneCache(long currentTime) {
        try {
            lock.writeLock().lock();
            long window = NOT_SET;
            if (getWindowTime() != NOT_SET) {
                window = currentTime - getWindowTime();
            }
            int count = 0;
            CacheItemImpl node = root;
            while (node != null) {
                if ((window != NOT_SET && node.timeStamp < window) || (windowCount != NOT_SET && size > windowCount)) {
                    CacheItemImpl remove = node;
                    node = node.getNext();
                    removeNode(remove);
                } else {
                    break;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void removeNode(CacheItemImpl node) {
        if (node != null) {
            set.remove(node.item);
            if (root == node) {
                root = node.getNext();
            }
            if (tail == node) {
                tail = node.getPrevious();
            }
            node.unlink();
            size--;
        }
    }

    protected class CacheItemImpl extends LinkedNode<CacheItemImpl> implements CacheItem<T> {
        T item;
        long timeStamp;

        public T getItem() {
            return item;
        }

        public long getTimestamp() {
            return timeStamp;
        }

        public int compareTo(CacheItem<T> CacheItem) {
            return (int) (this.timeStamp - CacheItem.getTimestamp());
        }
    }
}
