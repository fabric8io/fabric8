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
package org.apache.curator.framework.recipes.cache;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>A utility that attempts to keep all data from all children of a ZK path locally cached. This class
 * will watch the ZK path, respond to update/create/delete events, pull down the data, etc. You can
 * register a listener that will get notified when changes occur.</p>
 * <p/>
 * <p><b>IMPORTANT</b> - it's not possible to stay transactionally in sync. Users of this class must
 * be prepared for false-positives and false-negatives. Additionally, always use the version number
 * when updating data to avoid overwriting another process' change.</p>
 */
@SuppressWarnings("NullableProblems")
public class TreeCache implements Closeable
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final CuratorFramework client;
    private final String path;
    private final ExecutorService executorService;
    private final boolean cacheData;
    private final boolean diffData;
    private final boolean dataIsCompressed;
    private final EnsurePath ensurePath;
    private final BlockingQueue<Operation> operations = new PriorityBlockingQueue<Operation>(10, new OperationComparator());
    private final ListenerContainer<PathChildrenCacheListener> listeners = new ListenerContainer<PathChildrenCacheListener>();

    private final LoadingCache<String, TreeData> currentData = CacheBuilder.newBuilder().build(new CacheLoader<String, TreeData>() {
        @Override
        public TreeData load(String key) throws Exception {
            Stat stat = client.checkExists().forPath(key);
            if (stat!= null) {
                byte[] bytes = dataIsCompressed ? client.getData().decompressed().usingWatcher(watcher).forPath(key) : client.getData().usingWatcher(watcher).forPath(key);
                List<String> children = client.getChildren().usingWatcher(watcher).forPath(key);
                return  new TreeData(key, stat, bytes, children);
            } else {
                return null;
            }
        }
    });

    private final AtomicReference<Map<String, ChildData>> initialSet = new AtomicReference<Map<String, ChildData>>();

    private static final ChildData NULL_CHILD_DATA = new ChildData(null, null, null);
    private static final String CHILD_OF_ZNODE_PATTERN = "%s/[^ /]*";

    private final Watcher watcher = new Watcher() {
        @Override
        public void process(WatchedEvent event) {
            try {
                TreeData data;
                switch (event.getType()) {
                    case None:
                        break;
                    case NodeCreated:
                        break;
                    case NodeDeleted:
                        remove(event.getPath());
                        break;
                    case NodeDataChanged:
                        data = currentData.getIfPresent(event.getPath());
                        if (data != null) {
                            data.invalidate();
                        }
                        offerOperation(new GetDataFromTreeOperation(TreeCache.this, event.getPath()));
                        break;
                    case NodeChildrenChanged:
                        data = currentData.getIfPresent(event.getPath());
                        if (data != null) {
                            data.invalidate();
                        }
                        offerOperation(new TreeRefreshOperation(TreeCache.this, event.getPath(), RefreshMode.FORCE_GET_DATA_AND_STAT));
                }
            } catch (Exception e) {
                handleException(e);
            }
        }
    };

    @VisibleForTesting
    volatile Exchanger<Object> rebuildTestExchanger;

    private final ConnectionStateListener connectionStateListener = new ConnectionStateListener()
    {
        @Override
        public void stateChanged(CuratorFramework client, ConnectionState newState)
        {
            handleStateChange(newState);
        }
    };
    private static final ThreadFactory defaultThreadFactory = ThreadUtils.newThreadFactory("PathChildrenCache");

    /**
     * @param client the client
     * @param path   path to watch
     * @param mode   caching mode
     * @deprecated use {@link #TreeCache(org.apache.curator.framework.CuratorFramework, String, boolean)} instead
     */
    @SuppressWarnings("deprecation")
    public TreeCache(CuratorFramework client, String path, PathChildrenCacheMode mode)
    {
        this(client, path, mode != PathChildrenCacheMode.CACHE_PATHS_ONLY, false, Executors.newSingleThreadExecutor(defaultThreadFactory));
    }

    /**
     * @param client    the client
     * @param path      path to watch
     * @param cacheData if true, node contents are cached in addition to the stat
     */
    public TreeCache(CuratorFramework client, String path, boolean cacheData)
    {
        this(client, path, cacheData, false, Executors.newSingleThreadExecutor(defaultThreadFactory));
    }

    /**
     * @param client    the client
     * @param path      path to watch
     * @param cacheData if true, node contents are cached in addition to the stat
     */
    public TreeCache(CuratorFramework client, String path, boolean cacheData, boolean diffData)
    {
        this(client, path, cacheData, false, diffData, Executors.newSingleThreadExecutor(defaultThreadFactory));
    }

    /**
     * @param client        the client
     * @param path          path to watch
     * @param cacheData     if true, node contents are cached in addition to the stat
     * @param threadFactory factory to use when creating internal threads
     */
    public TreeCache(CuratorFramework client, String path, boolean cacheData, ThreadFactory threadFactory)
    {
        this(client, path, cacheData,  false, Executors.newSingleThreadExecutor(threadFactory));
    }

    /**
     * @param client           the client
     * @param path             path to watch
     * @param cacheData        if true, node contents are cached in addition to the stat
     * @param dataIsCompressed if true, data in the path is compressed
     * @param threadFactory    factory to use when creating internal threads
     */
    public TreeCache(CuratorFramework client, String path, boolean cacheData, boolean dataIsCompressed, ThreadFactory threadFactory)
    {
        this(client, path, cacheData, dataIsCompressed, Executors.newSingleThreadExecutor(threadFactory));
    }

    /**
     * @param client           the client
     * @param path             path to watch
     * @param cacheData        if true, node contents are cached in addition to the stat
     * @param dataIsCompressed if true, data in the path is compressed
     * @param executorService  ExecutorService to use for the PathChildrenCache's background thread
     */
    public TreeCache(CuratorFramework client, String path, boolean cacheData, boolean dataIsCompressed, final ExecutorService executorService)
    {
        this(client, path, cacheData, dataIsCompressed, false, executorService);
    }

    /**
     * @param client           the client
     * @param path             path to watch
     * @param cacheData        if true, node contents are cached in addition to the stat
     * @param dataIsCompressed if true, data in the path is compressed
     * @param executorService  ExecutorService to use for the PathChildrenCache's background thread
     */
    public TreeCache(CuratorFramework client, String path, boolean cacheData, boolean dataIsCompressed, boolean diffData, final ExecutorService executorService)
    {
        this.client = client;
        this.path = path;
        this.cacheData = cacheData;
        this.diffData = diffData;
        this.dataIsCompressed = dataIsCompressed;
        this.executorService = executorService;
        ensurePath = client.newNamespaceAwareEnsurePath(path);
    }

    /**
     * Start the cache. The cache is not started automatically. You must call this method.
     *
     * @throws Exception errors
     */
    public void start() throws Exception
    {
        start(StartMode.NORMAL);
    }

    /**
     * Same as {@link #start()} but gives the option of doing an initial build
     *
     * @param buildInitial if true, {@link #rebuild()} will be called before this method
     *                     returns in order to get an initial view of the node; otherwise,
     *                     the cache will be initialized asynchronously
     * @deprecated use {@link #start(StartMode)}
     * @throws Exception errors
     */
    public void start(boolean buildInitial) throws Exception
    {
        start(buildInitial ? StartMode.BUILD_INITIAL_CACHE : StartMode.NORMAL);
    }

    /**
     * Method of priming cache on {@link TreeCache#start(StartMode)}
     */
    public enum StartMode
    {
        /**
         * cache will _not_ be primed. i.e. it will start empty and you will receive
         * events for all nodes added, etc.
         */
        NORMAL,

        /**
         * {@link TreeCache#rebuild()} will be called before this method returns in
         * order to get an initial view of the node.
         */
        BUILD_INITIAL_CACHE,

        /**
         * After cache is primed with initial values (in the background) a
         * {@link PathChildrenCacheEvent.Type#INITIALIZED} will be posted
         */
        POST_INITIALIZED_EVENT
    }

    /**
     * Start the cache. The cache is not started automatically. You must call this method.
     *
     * @param mode Method for priming the cache
     * @throws Exception errors
     */
    public void start(StartMode mode) throws Exception
    {
        Preconditions.checkState(!executorService.isShutdown(), "already started");
        mode = Preconditions.checkNotNull(mode, "mode cannot be null");

        client.getConnectionStateListenable().addListener(connectionStateListener);
        executorService.execute
            (
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mainLoop();
                    }
                }
            );

        switch ( mode )
        {
            case NORMAL:
            {
                offerOperation(new TreeRefreshOperation(this, path, RefreshMode.STANDARD));
                break;
            }

            case BUILD_INITIAL_CACHE:
            {
                rebuild();
                break;
            }

            case POST_INITIALIZED_EVENT:
            {
                initialSet.set(Maps.<String, ChildData>newConcurrentMap());
                offerOperation(new TreeRefreshOperation(this, path, RefreshMode.POST_INITIALIZED));
                break;
            }
        }
    }

    /**
     * NOTE: this is a BLOCKING method. Completely rebuild the internal cache by querying
     * for all needed data WITHOUT generating any events to send to listeners.
     *
     * @throws Exception errors
     */
    public void rebuild() throws Exception
    {
        Preconditions.checkState(!executorService.isShutdown(), "cache has been closed");

        ensurePath.ensure(client.getZookeeperClient());

        clear();

        List<String> children = client.getChildren().forPath(path);
        for ( String child : children )
        {
            String fullPath = ZKPaths.makePath(path, child);
            internalRebuildNode(fullPath);

            if ( rebuildTestExchanger != null )
            {
                rebuildTestExchanger.exchange(new Object());
            }
        }

        // this is necessary so that any updates that occurred while rebuilding are taken
        offerOperation(new TreeRefreshOperation(this, path, RefreshMode.FORCE_GET_DATA_AND_STAT));
    }

    /**
     * NOTE: this is a BLOCKING method. Rebuild the internal cache for the given node by querying
     * for all needed data WITHOUT generating any events to send to listeners.
     *
     * @param fullPath full path of the node to rebuild
     * @throws Exception errors
     */
    public void rebuildNode(String fullPath) throws Exception
    {
        Preconditions.checkArgument(ZKPaths.getPathAndNode(fullPath).getPath().startsWith(path), "Node is not part of this cache: " + fullPath);
        Preconditions.checkState(!executorService.isShutdown(), "cache has been closed");

        ensurePath.ensure(client.getZookeeperClient());
        internalRebuildNode(fullPath);

        // this is necessary so that any updates that occurred while rebuilding are taken
        // have to rebuild entire tree in case this node got deleted in the interim
        offerOperation(new TreeRefreshOperation(this, path, RefreshMode.FORCE_GET_DATA_AND_STAT));
    }

    /**
     * Close/end the cache
     *
     * @throws java.io.IOException errors
     */
    @Override
    public void close() throws IOException
    {
        //Preconditions.checkState(!executorService.isShutdown(), "has not been started");

        client.getConnectionStateListenable().removeListener(connectionStateListener);
        executorService.shutdownNow();
    }

    /**
     * Return the cache listenable
     *
     * @return listenable
     */
    public ListenerContainer<PathChildrenCacheListener> getListenable()
    {
        return listeners;
    }

    /**
     * Return the current data. There are no guarantees of accuracy. This is
     * merely the most recent view of the data. The data is returned in sorted order.
     *
     * @return list of children and data
     */
    public List<TreeData> getCurrentData()
    {
        return ImmutableList.copyOf(Sets.<TreeData>newTreeSet(currentData.asMap().values()));
    }

    /**
     * Return the current data for the given path. There are no guarantees of accuracy. This is
     * merely the most recent view of the data. If there is no child with that path, <code>null</code>
     * is returned.
     *
     * @param fullPath full path to the node to check
     * @return data or null
     */
    public TreeData getCurrentData(String fullPath)
    {
        try {
            while (true) {
                TreeData data = currentData.get(fullPath);
                if (data.isInvalidated()) {
                    currentData.invalidate(fullPath);
                } else {
                    return data;
                }
            }
        } catch (ExecutionException e) {
            return null;
        } catch (CacheLoader.InvalidCacheLoadException e) {
            return null;
        }
    }


    public List<TreeData> getChildren(String fullPath)
    {
        List<TreeData> children = Lists.newArrayList();
        for (String child : getChildrenNames(fullPath)) {
            String childFullPath = ZKPaths.makePath(fullPath, child);
            children.add(getCurrentData(childFullPath));
        }
        return children;
    }

    public List<String> getChildrenNames(String fullPath)
    {
        TreeData parentData = getCurrentData(fullPath);
        if (parentData != null) {
            return new ArrayList<String>(parentData.getChildren());
        }
        return Lists.newArrayList();
    }

    /**
     * As a memory optimization, you can clear the cached data bytes for a node. Subsequent
     * calls to {@link ChildData#getData()} for this node will return <code>null</code>.
     *
     * @param fullPath the path of the node to clear
     */
    public void clearDataBytes(String fullPath)
    {
        clearDataBytes(fullPath, -1);
    }

    /**
     * As a memory optimization, you can clear the cached data bytes for a node. Subsequent
     * calls to {@link ChildData#getData()} for this node will return <code>null</code>.
     *
     * @param fullPath  the path of the node to clear
     * @param ifVersion if non-negative, only clear the data if the data's version matches this version
     * @return true if the data was cleared
     */
    public boolean clearDataBytes(String fullPath, int ifVersion)
    {
        TreeData data = currentData.getIfPresent(fullPath);
        if ( data != null )
        {
            if ( (ifVersion < 0) || (ifVersion == data.getStat().getVersion()) )
            {
                data.clearData();
                return true;
            }
        }
        return false;
    }

    /**
     * Clear out current data and begin a new query on the path
     *
     * @throws Exception errors
     */
    public void clearAndRefresh() throws Exception
    {
        currentData.invalidateAll();
        offerOperation(new TreeRefreshOperation(this, path, RefreshMode.STANDARD));
    }

    /**
     * Clears the current data without beginning a new query and without generating any events
     * for listeners.
     */
    public void clear()
    {
        currentData.invalidateAll();
    }

    enum RefreshMode
    {
        STANDARD,
        FORCE_GET_DATA_AND_STAT,
        POST_INITIALIZED
    }

    void refresh(final String path, final RefreshMode mode) throws Exception
    {
        ensurePath.ensure(client.getZookeeperClient());
        Stat stat = new Stat();
        List<String> children = client.getChildren().storingStatIn(stat).usingWatcher(watcher).forPath(path);
        processChildren(path, children, mode);
        updateIfNeeded(path, stat, children);
    }

    void callListeners(final PathChildrenCacheEvent event)
    {
        listeners.forEach
                (
                        new Function<PathChildrenCacheListener, Void>() {
                            @Override
                            public Void apply(PathChildrenCacheListener listener) {
                                try {
                                    listener.childEvent(client, event);
                                } catch (Exception e) {
                                    handleException(e);
                                }
                                return null;
                            }
                        }
                );
    }

    void getDataAndStat(final String fullPath) throws Exception
    {
        if (client.checkExists().forPath(fullPath) == null) {
            return;
        }

        final List<String> children = client.getChildren().usingWatcher(watcher).forPath(fullPath);
        if ( cacheData )
        {
            Stat stat = new Stat();
            byte[] data = null;
            if ( dataIsCompressed )
            {
                data = client.getData().decompressed().storingStatIn(stat).usingWatcher(watcher).forPath(fullPath);

            }
            else
            {
                data = client.getData().storingStatIn(stat).usingWatcher(watcher).forPath(fullPath);
            }
            applyNewData(fullPath, KeeperException.Code.OK.intValue(), stat, data, children);
        }
        else
        {
            Stat stat = client.checkExists().usingWatcher(watcher).forPath(fullPath);
            applyNewData(fullPath, KeeperException.Code.OK.intValue(), stat, null, children);
        }
    }

    /**
     * Default behavior is just to log the exception
     *
     * @param e the exception
     */
    protected void handleException(Throwable e)
    {
        log.error("", e);
    }

    @VisibleForTesting
    protected void remove(String fullPath)
    {
        TreeData data = currentData.getIfPresent(fullPath);
        if ( data != null )
        {
            currentData.invalidate(fullPath);
            offerOperation(new TreeEventOperation(this, new PathChildrenCacheEvent(PathChildrenCacheEvent.Type.CHILD_REMOVED, data)));
        }
        Map<String, ChildData> localInitialSet = initialSet.get();
        if ( localInitialSet != null )
        {
            localInitialSet.remove(fullPath);
            maybeOfferInitializedEvent(localInitialSet);
        }
        removeFromParent(fullPath);
    }

    private void internalRebuildNode(String fullPath) throws Exception
    {
        if ( cacheData )
        {
            try
            {
                Stat stat = new Stat();
                byte[] bytes = dataIsCompressed ? client.getData().decompressed().storingStatIn(stat).forPath(fullPath) : client.getData().storingStatIn(stat).forPath(fullPath);
                List<String> children = client.getChildren().forPath(fullPath);
                currentData.put(fullPath, new TreeData(fullPath, stat, bytes, children));
                for (String child : children) {
                    String childPath = ZKPaths.makePath(fullPath, child);
                    internalRebuildNode(childPath);
                }
            }
            catch ( KeeperException.NoNodeException ignore )
            {
                // node no longer exists - remove it
                currentData.invalidate(fullPath);
                removeFromParent(fullPath);
            }
        }
        else
        {
            Stat stat = client.checkExists().forPath(fullPath);
            if ( stat != null )
            {
                List<String> children = client.getChildren().forPath(fullPath);
                currentData.put(fullPath, new TreeData(fullPath, stat, null, children));
                for (String child : children) {
                    String childPath = ZKPaths.makePath(fullPath, child);
                    internalRebuildNode(childPath);
                }
            }
            else
            {
                // node no longer exists - remove it
                currentData.invalidate(fullPath);
                removeFromParent(fullPath);
            }
        }
    }

    private void handleStateChange(ConnectionState newState)
    {
        switch ( newState )
        {
        case SUSPENDED:
        {
            offerOperation(new TreeEventOperation(this, new PathChildrenCacheEvent(PathChildrenCacheEvent.Type.CONNECTION_SUSPENDED, null)));
            break;
        }

        case LOST:
        {
            offerOperation(new TreeEventOperation(this, new PathChildrenCacheEvent(PathChildrenCacheEvent.Type.CONNECTION_LOST, null)));
            break;
        }

        case RECONNECTED:
        {
            try
            {
                offerOperation(new TreeRefreshOperation(this, path, RefreshMode.FORCE_GET_DATA_AND_STAT));
                offerOperation(new TreeEventOperation(this, new PathChildrenCacheEvent(PathChildrenCacheEvent.Type.CONNECTION_RECONNECTED, null)));
            }
            catch ( Exception e )
            {
                handleException(e);
            }
            break;
        }
        }
    }

    private void processChildren(final String path, List<String> children, RefreshMode mode) throws Exception
    {
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

        Set<String> removedNodes = Sets.filter(Sets.newHashSet(currentData.asMap().keySet()), new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                if (input.matches(String.format(CHILD_OF_ZNODE_PATTERN, path))) {
                    return true;
                } else {
                    return false;
                }
            }
        });

        removedNodes.removeAll(fullPaths);

        for ( String fullPath : removedNodes )
        {
            remove(fullPath);
        }

        for ( String name : children )
        {
            String fullPath = ZKPaths.makePath(path, name);

            if ( (mode == RefreshMode.FORCE_GET_DATA_AND_STAT) || currentData.getIfPresent(fullPath) == null )
            {
                getDataAndStat(fullPath);
            }
            updateInitialSet(fullPath, NULL_CHILD_DATA);
            offerOperation(new TreeRefreshOperation(this, fullPath, mode));
        }
        maybeOfferInitializedEvent(initialSet.get());
    }

    private void updateIfNeeded(String path, Stat stat, List<String> children) throws Exception {
        TreeData data = currentData.getIfPresent(path);
        if (data != null && stat != null) {
            if (stat.getMzxid() > data.getStat().getMzxid()) {
                    applyNewData(path, KeeperException.Code.OK.intValue(), stat, data.getData(), children);
            }
        }
    }

    private synchronized void addToParent(String fullPath) {
        Optional<String> parent = getParentOf(fullPath);
        if (parent.isPresent()) {
            TreeData parentData = currentData.getIfPresent(parent.get());
            if (parentData != null) {
                parentData.getChildren().add(ZKPaths.getNodeFromPath(fullPath));
            }
        }
    }

    private synchronized void removeFromParent(String fullPath) {
        Optional<String> parent = getParentOf(fullPath);
        if (parent.isPresent()) {
            TreeData parentData = currentData.getIfPresent(parent.get());
            if (parentData != null) {
                parentData.getChildren().remove(ZKPaths.getNodeFromPath(fullPath));
            }
        }
    }

    private void applyNewData(String fullPath, int resultCode, Stat stat, byte[] bytes, List<String> children)
    {
        if ( resultCode == KeeperException.Code.OK.intValue() ) // otherwise - node must have dropped or something - we should be getting another event
        {
            TreeData data = new TreeData(fullPath, stat, bytes, children);
            TreeData previousData;

            synchronized (this) {
                previousData = currentData.getIfPresent(fullPath);
                currentData.put(fullPath, data);
                addToParent(fullPath);
            }

            if ( previousData == null ) // i.e. new
            {
                offerOperation(new TreeEventOperation(this, new PathChildrenCacheEvent(PathChildrenCacheEvent.Type.CHILD_ADDED, data)));
            }
            else if ( previousData.getStat().getVersion() != stat.getVersion() )
            {
                if (!diffData || !Arrays.equals(data.getData(), previousData.getData())) {
                    offerOperation(new TreeEventOperation(this, new PathChildrenCacheEvent(PathChildrenCacheEvent.Type.CHILD_UPDATED, data)));
                }
            }
            updateInitialSet(ZKPaths.getNodeFromPath(fullPath), data);
        }
    }

    private void updateInitialSet(String name, ChildData data)
    {
        Map<String, ChildData> localInitialSet = initialSet.get();
        if ( localInitialSet != null )
        {
            localInitialSet.put(name, data);
            maybeOfferInitializedEvent(localInitialSet);
        }
    }

    private void maybeOfferInitializedEvent(Map<String, ChildData> localInitialSet)
    {
        if ( !hasUninitialized(localInitialSet) )
        {
            // all initial children have been processed - send initialized message

            if ( initialSet.getAndSet(null) != null )   // avoid edge case - don't send more than 1 INITIALIZED event
            {
                final List<ChildData> children = ImmutableList.copyOf(localInitialSet.values());
                PathChildrenCacheEvent event = new PathChildrenCacheEvent(PathChildrenCacheEvent.Type.INITIALIZED, null)
                {
                    @Override
                    public List<ChildData> getInitialData()
                    {
                        return children;
                    }
                };
                offerOperation(new TreeEventOperation(this, event));
            }
        }
    }

    private boolean hasUninitialized(Map<String, ChildData> localInitialSet)
    {
        if ( localInitialSet == null )
        {
            return false;
        }

        Map<String, ChildData> uninitializedChildren = Maps.filterValues
        (
                localInitialSet,
                new Predicate<ChildData>() {
                    @Override
                    public boolean apply(ChildData input) {
                        return (input == NULL_CHILD_DATA);  // check against ref intentional
                    }
                }
        );
        return (uninitializedChildren.size() != 0);
    }

    private void mainLoop()
    {
        while ( !Thread.currentThread().isInterrupted() )
        {
            try
            {
                operations.take().invoke();
            }
            catch ( InterruptedException e )
            {
                Thread.currentThread().interrupt();
                break;
            }
            catch ( Exception e )
            {
                handleException(e);
            }
        }
    }

    private void offerOperation(Operation operation)
    {
        operations.remove(operation);   // avoids herding for refresh operations
        operations.offer(operation);
    }

    private Optional<String> getParentOf(String path) {
        if (path == null || path.equals("/")) {
            return Optional.absent();
        } else {
            return Optional.of(path.substring(0, path.lastIndexOf("/")));
        }
    }
}
