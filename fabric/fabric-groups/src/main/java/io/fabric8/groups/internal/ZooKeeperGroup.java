/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.fabric8.groups.internal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.listen.ListenerContainer;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.utils.EnsurePath;
import org.apache.curator.utils.ThreadUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.codehaus.jackson.map.ObjectMapper;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.NodeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>A utility that attempts to keep all data from all children of a ZK path locally cached. This class
 * will watch the ZK path, respond to update/create/delete events, pull down the data, etc. You can
 * register a listener that will get notified when changes occur.</p>
 * <p/>
 * <p><b>IMPORTANT</b> - it's not possible to stay transactionally in sync. Users of this class must
 * be prepared for false-positives and false-negatives. Additionally, always use the version number
 * when updating data to avoid overwriting another process' change.</p>
 */
public class ZooKeeperGroup<T extends NodeState> implements Group<T> {

    static public final ObjectMapper MAPPER = new ObjectMapper();

    static private final Logger LOG = LoggerFactory.getLogger(ZooKeeperGroup.class);

    private final Class<T> clazz;
    private final CuratorFramework client;
    private final String path;
    private final ExecutorService executorService;
    private final EnsurePath ensurePath;
    private final BlockingQueue<Operation> operations = new LinkedBlockingQueue<Operation>();
    private final ListenerContainer<GroupListener<T>> listeners = new ListenerContainer<GroupListener<T>>();
    protected final ConcurrentMap<String, ChildData<T>> currentData = Maps.newConcurrentMap();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean connected = new AtomicBoolean();
    protected final SequenceComparator sequenceComparator = new SequenceComparator();

    private volatile String id;
    private T state;

    private final Watcher childrenWatcher = new Watcher() {
        @Override
        public void process(WatchedEvent event) {
            offerOperation(new RefreshOperation(ZooKeeperGroup.this, RefreshMode.STANDARD));
        }
    };

    private final Watcher dataWatcher = new Watcher() {
        @Override
        public void process(WatchedEvent event) {
            try {
                if (event.getType() == Event.EventType.NodeDeleted) {
                    remove(event.getPath());
                } else if (event.getType() == Event.EventType.NodeDataChanged) {
                    offerOperation(new GetDataOperation(ZooKeeperGroup.this, event.getPath()));
                }
            } catch (Exception e) {
                handleException(e);
            }
        }
    };

    private final ConnectionStateListener connectionStateListener = new ConnectionStateListener() {
        @Override
        public void stateChanged(CuratorFramework client, ConnectionState newState) {
            handleStateChange(newState);
        }
    };

    /**
     * @param client the client
     * @param path   path to watch
     */
    public ZooKeeperGroup(CuratorFramework client, String path, Class<T> clazz) {
        this(client, path, clazz,
                Executors.newSingleThreadExecutor(ThreadUtils.newThreadFactory("ZooKeeperGroup")));
    }

    /**
     * @param client        the client
     * @param path          path to watch
     * @param threadFactory factory to use when creating internal threads
     */
    public ZooKeeperGroup(CuratorFramework client, String path, Class<T> clazz, ThreadFactory threadFactory) {
        this(client, path, clazz, Executors.newSingleThreadExecutor(threadFactory));
    }

    /**
     * @param client          the client
     * @param path            path to watch
     * @param executorService ExecutorService to use for the ZooKeeperGroup's background thread
     */
    public ZooKeeperGroup(CuratorFramework client, String path, Class<T> clazz, final ExecutorService executorService) {
        this.client = client;
        this.path = path;
        this.clazz = clazz;
        this.executorService = executorService;
        ensurePath = client.newNamespaceAwareEnsurePath(path);
    }

    /**
     * Start the cache. The cache is not started automatically. You must call this method.
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            connected.set(client.getZookeeperClient().isConnected());
            client.getConnectionStateListenable().addListener(connectionStateListener);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    mainLoop();
                }
            });

            if (isConnected()) {
                handleStateChange(ConnectionState.CONNECTED);
            }
        }
    }

    /**
     * Close/end the cache
     *
     * @throws IOException errors
     */
    @Override
    public void close() throws IOException {
        if (started.compareAndSet(true, false)) {
            client.getConnectionStateListenable().removeListener(connectionStateListener);
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw (IOException) new InterruptedIOException().initCause(e);
            }
            try {
                if (isConnected()) {
                    doUpdate(null);
                    callListeners(GroupListener.GroupEvent.DISCONNECTED);
                }
            } catch (Exception e) {
                handleException(e);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public void add(GroupListener<T> listener) {
        listeners.addListener(listener);
    }

    @Override
    public void remove(GroupListener<T> listener) {
        listeners.removeListener(listener);
    }

    @Override
    public void update(T state) {
        T oldState = this.state;
        this.state = state;

        if (started.get()) {
            boolean update = state == null && oldState != null
                        ||   state != null && oldState == null
                        || !Arrays.equals(encode(state), encode(oldState));
            if (update) {
                offerOperation(new RefreshOperation(this, RefreshMode.FORCE_GET_DATA_AND_STAT));
                offerOperation(new UpdateOperation<T>(this, state));
            }
        }
    }

    protected void doUpdate(T state) throws Exception {
        if (isConnected()) {
            if (state == null) {
                if (id != null) {
                    try {
                        client.delete().guaranteed().forPath(id);
                    } catch (KeeperException.NoNodeException e) {
                        // Ignore
                    } finally {
                        id = null;
                    }
                }
            } else {
                if (id == null) {
                    //We explicitly refresh to prevent members() from returning stale data.
                    refresh(RefreshMode.FORCE_GET_DATA_AND_STAT);
                    // We could have created the sequence, but then have crashed and our entry is already registered,
                    // find out by looking up entry by the matching uuid.
                    Map<String, T> members = members();
                    for (Map.Entry<String, T> entry : members.entrySet()) {
                        T v = entry.getValue();
                        if( state.getContainer().equals(v.getContainer()) ) {
                            id = entry.getKey();
                            return;
                        }
                    }
                }

                if (id == null) {
                    id = client.create().creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                        .forPath(path + "/0", encode(state));
                } else {
                    try {
                        client.setData().forPath(id, encode(state));
                    } catch (KeeperException.NoNodeException e) {
                        id = client.create().creatingParentsIfNeeded()
                                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                                .forPath(path + "/0", encode(state));
                    }
                }
            }
        }
    }

    @Override
    public Map<String, T> members() {
        List<ChildData<T>> children = new ArrayList<ChildData<T>>(currentData.values());
        Collections.sort(children, sequenceComparator);
        Map<String, T> members = new LinkedHashMap<String, T>();
        for (ChildData<T> child : children) {
            members.put(child.getPath(), child.getNode());
        }
        return members;
    }

    @Override
    public boolean isMaster() {
        List<ChildData<T>> children = new ArrayList<ChildData<T>>(currentData.values());
        Collections.sort(children, sequenceComparator);
        return (!children.isEmpty() && children.get(0).getPath().equals(id));
    }

    @Override
    public T master() {
        List<ChildData<T>> children = new ArrayList<ChildData<T>>(currentData.values());
        Collections.sort(children, sequenceComparator);
        if (children.isEmpty()) {
            return null;
        }
        return children.get(0).getNode();
    }

    @Override
    public List<T> slaves() {
        List<ChildData<T>> children = new ArrayList<ChildData<T>>(currentData.values());
        Collections.sort(children, sequenceComparator);
        List<T> slaves = new ArrayList<T>();
        for (int i = 1; i < children.size(); i++) {
            slaves.add(children.get(i).getNode());
        }
        return slaves;
    }


    /**
     * Return the cache listenable
     *
     * @return listenable
     */
    public ListenerContainer<GroupListener<T>> getListenable() {
        return listeners;
    }

    /**
     * Return the current data. There are no guarantees of accuracy. This is
     * merely the most recent view of the data. The data is returned in sorted order.
     *
     * @return list of children and data
     */
    public List<ChildData> getCurrentData() {
        return ImmutableList.copyOf(Sets.<ChildData>newTreeSet(currentData.values()));
    }

    /**
     * Return the current data for the given path. There are no guarantees of accuracy. This is
     * merely the most recent view of the data. If there is no child with that path, <code>null</code>
     * is returned.
     *
     * @param fullPath full path to the node to check
     * @return data or null
     */
    public ChildData getCurrentData(String fullPath) {
        return currentData.get(fullPath);
    }

    /**
     * Clear out current data and begin a new query on the path
     *
     * @throws Exception errors
     */
    public void clearAndRefresh() throws Exception {
        currentData.clear();
        offerOperation(new RefreshOperation(this, RefreshMode.STANDARD));
    }

    /**
     * Clears the current data without beginning a new query and without generating any events
     * for listeners.
     */
    public void clear() {
        currentData.clear();
    }

    enum RefreshMode {
        STANDARD,
        FORCE_GET_DATA_AND_STAT
    }

    void refresh(final RefreshMode mode) throws Exception {
        ensurePath.ensure(client.getZookeeperClient());
        List<String> children = client.getChildren().usingWatcher(childrenWatcher).forPath(path);
        Collections.sort(children, new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                return left.compareTo(right);
            }
        });
        processChildren(children, mode);
    }

    void callListeners(final GroupListener.GroupEvent event) {
        listeners.forEach
                (
                        new Function<GroupListener<T>, Void>() {
                            @Override
                            public Void apply(GroupListener<T> listener) {
                                try {
                                    listener.groupEvent(ZooKeeperGroup.this, event);
                                } catch (Exception e) {
                                    handleException(e);
                                }
                                return null;
                            }
                        }
                );
    }

    void getDataAndStat(final String fullPath) throws Exception {
        Stat stat = new Stat();
        byte[] data = client.getData().storingStatIn(stat).usingWatcher(dataWatcher).forPath(fullPath);
        applyNewData(fullPath, KeeperException.Code.OK.intValue(), stat, data);
    }

    /**
     * Default behavior is just to log the exception
     *
     * @param e the exception
     */
    protected void handleException(Throwable e) {
        LOG.error("", e);
    }

    @VisibleForTesting
    protected void remove(String fullPath) {
        ChildData data = currentData.remove(fullPath);
        if (data != null) {
            offerOperation(new EventOperation(this, GroupListener.GroupEvent.CHANGED));
        }
    }

    private void internalRebuildNode(String fullPath) throws Exception {
        try {
            Stat stat = new Stat();
            byte[] bytes = client.getData().storingStatIn(stat).forPath(fullPath);
            currentData.put(fullPath, new ChildData<T>(fullPath, stat, bytes, decode(bytes)));
        } catch (KeeperException.NoNodeException ignore) {
            // node no longer exists - remove it
            currentData.remove(fullPath);
        }
    }

    private void handleStateChange(ConnectionState newState) {
        switch (newState) {
            case SUSPENDED:
            case LOST: {
                connected.set(false);
                clear();
                offerOperation(new EventOperation(this, GroupListener.GroupEvent.DISCONNECTED));
                break;
            }

            case CONNECTED:
            case RECONNECTED: {
                connected.set(true);
                offerOperation(new RefreshOperation(this, RefreshMode.FORCE_GET_DATA_AND_STAT));
                offerOperation(new UpdateOperation<T>(this, state));
                offerOperation(new EventOperation(this, GroupListener.GroupEvent.CONNECTED));
                break;
            }
        }
    }

    private void processChildren(List<String> children, RefreshMode mode) throws Exception {
        List<String> fullPaths = Lists.newArrayList(Lists.transform
                (
                        children,
                        new Function<String, String>() {
                            @Override
                            public String apply(String child) {
                                return ZKPaths.makePath(path, child);
                            }
                        }
                ));
        Set<String> removedNodes = Sets.newHashSet(currentData.keySet());
        removedNodes.removeAll(fullPaths);

        for (String fullPath : removedNodes) {
            remove(fullPath);
        }

        for (String name : children) {
            String fullPath = ZKPaths.makePath(path, name);

            if ((mode == RefreshMode.FORCE_GET_DATA_AND_STAT) || !currentData.containsKey(fullPath)) {
                getDataAndStat(fullPath);
            }
        }
    }

    private void applyNewData(String fullPath, int resultCode, Stat stat, byte[] bytes) {
        if (resultCode == KeeperException.Code.OK.intValue()) {
            // otherwise - node must have dropped or something - we should be getting another event
            ChildData<T> data = new ChildData<T>(fullPath, stat, bytes, decode(bytes));
            ChildData<T> previousData = currentData.put(fullPath, data);
            if (previousData == null || previousData.getStat().getVersion() != stat.getVersion()) {
                offerOperation(new EventOperation(this, GroupListener.GroupEvent.CHANGED));
            }
        }
    }

    private void mainLoop() {
        while (started.get() && !Thread.currentThread().isInterrupted()) {
            try {
                operations.take().invoke();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                handleException(e);
            }
        }
    }

    private byte[] encode(T state) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MAPPER.writeValue(baos, state);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to decode data", e);
        }
    }

    private T decode(byte[] data) {
        try {
            return MAPPER.readValue(data, clazz);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to decode data", e);
        }
    }

    private void offerOperation(Operation operation) {
        operations.remove(operation);   // avoids herding for refresh operations
        operations.offer(operation);
    }

    public static <T> Map<String, T> members(CuratorFramework curator, String path, Class<T> clazz) throws Exception {
        Map<String, T> map = new TreeMap<String, T>();
        List<String> nodes = curator.getChildren().forPath(path);
        ObjectMapper mapper = new ObjectMapper();
        for (String node : nodes) {
            byte[] data = curator.getData().forPath(path + "/" + node);
            T val = mapper.readValue(data, clazz);
            map.put(node, val);
        }
        return map;
    }

    public String getId() {
        return id;
    }
}
