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

package org.fusesource.insight.metrics;


import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.NodeState;
import io.fabric8.groups.internal.TrackingZooKeeperGroup;
import org.fusesource.insight.metrics.model.MBeanAttrs;
import org.fusesource.insight.metrics.model.MBeanOpers;
import org.fusesource.insight.metrics.model.Query;
import org.fusesource.insight.metrics.model.QueryResult;
import org.fusesource.insight.metrics.model.Request;
import org.fusesource.insight.metrics.model.Server;
import org.fusesource.insight.metrics.support.JmxUtils;
import org.fusesource.insight.metrics.support.Renderer;
import org.fusesource.insight.metrics.support.ScriptUtils;
import org.fusesource.insight.storage.StorageService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.fusesource.insight.metrics.support.IoUtils.loadFully;
import static org.fusesource.insight.metrics.support.ScriptUtils.parseJson;

/**
 * Collects all the charting metrics defined against its profiles
 */
public class MetricsCollector implements MetricsCollectorMBean {

    public static final String GRAPH_JSON = "org.fusesource.insight.metrics.json";

    public static final String QUERIES = "queries";
    public static final String NAME = "name";
    public static final String TEMPLATE = "template";
    public static final String METADATA = "metadata";
    public static final String LOCK = "lock";
    public static final String PERIOD = "period";
    public static final String MIN_PERIOD = "minPeriod";
    public static final String REQUESTS = "requests";
    public static final String OBJ = "obj";
    public static final String ATTRS = "attrs";
    public static final String OPER = "oper";
    public static final String ARGS = "args";
    public static final String SIG = "sig";
    public static final String DEFAULT = "default";
    public static final String LOCK_GLOBAL = "global";
    public static final String LOCK_HOST = "host";

    private static final transient Logger LOG = LoggerFactory.getLogger(MetricsCollector.class);

    private ObjectName objectName;

    private BundleContext bundleContext;
    private FabricService fabricService;

    private ScheduledThreadPoolExecutor executor;
    private Map<Query, QueryState> queries = new ConcurrentHashMap<Query, QueryState>();
    private Renderer renderer = new Renderer();

    private ServiceTracker<MBeanServer, MBeanServer> mbeanServer;
    private ServiceTracker<StorageService, StorageService> storage;

    private int defaultDelay = 60;
    private int threadPoolSize = 5;
    private String type;

    static class QueryState {
        ScheduledFuture<?> future;
        Server server;
        Query query;
        QueryResult lastResult;
        boolean lastResultSent;
        long lastSent;
        Map metadata;
        Group<QueryNodeState> lock;

        public void close() {
            future.cancel(false);
            if (lock != null) {
                try {
                    lock.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    static class QueryNodeState extends NodeState {
        @JsonProperty
        String[] services;

        QueryNodeState() {
        }

        QueryNodeState(String id, String container, String[] services) {
            super(id, container);
            this.services = services;
        }

    }

    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setDefaultDelay(int defaultDelay) {
        this.defaultDelay = defaultDelay;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    public String getMetrics() {
        Map<String, Object> meta = new HashMap<String, Object>();
        for (Map.Entry<Query, QueryState> e : queries.entrySet()) {
            meta.put(e.getKey().getName(), e.getValue().metadata);
        }
        return ScriptUtils.toJson(meta);
    }

    public void start() throws IOException {
        this.executor = new ScheduledThreadPoolExecutor(threadPoolSize);
        this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);

        this.mbeanServer = new ServiceTracker<MBeanServer, MBeanServer>(bundleContext, MBeanServer.class, new ServiceTrackerCustomizer<MBeanServer, MBeanServer>() {
            @Override
            public MBeanServer addingService(ServiceReference<MBeanServer> reference) {
                MBeanServer service = bundleContext.getService(reference);
                try {
                    service.registerMBean(MetricsCollector.this, objectName);
                } catch (Exception e) {
                    LOG.info("Unable to register metrics collector mbean", e);
                }
                return service;
            }

            @Override
            public void modifiedService(ServiceReference<MBeanServer> reference, MBeanServer service) {
            }

            @Override
            public void removedService(ServiceReference<MBeanServer> reference, MBeanServer service) {
                try {
                    service.unregisterMBean(objectName);
                } catch (Exception e) {
                    LOG.info("Unable to unregister metrics collector mbean", e);
                }
                bundleContext.ungetService(reference);
            }
        });
        this.storage = new ServiceTracker<StorageService, StorageService>(bundleContext, StorageService.class, null);

        this.mbeanServer.open();
        this.storage.open();

        this.executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                process();
            }
        }, 1, defaultDelay, TimeUnit.SECONDS);
    }

    public void stop() throws Exception {
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Ignore
        }
        this.executor.shutdownNow();
        try {
            this.executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Ignore
        }
        this.mbeanServer.close();
        this.storage.close();
        for (QueryState q : queries.values()) {
            q.close();
        }
    }


    public void process() {
        try {
            Container container = fabricService.getCurrentContainer();
            if (container != null) {
                Set<Query> newQueries = new HashSet<Query>();
                Profile[] profiles = container.getProfiles();
                if (profiles != null) {
                    for (Profile profile : profiles) {
                        loadProfile(profile, newQueries);
                    }
                }
                for (Query q : queries.keySet()) {
                    if (!newQueries.remove(q)) {
                        queries.remove(q).close();
                    }
                }
                Server server = new Server(container.getId());
                for (Query q : newQueries) {
                    final String queryName = q.getName();
                    final String containerName = container.getId();
                    final QueryState state = new QueryState();
                    state.server = server;
                    state.query = q;
                    if (q.getMetadata() != null) {
                        state.metadata = parseJson(loadFully(new URL(q.getMetadata())));
                    }

                    // Clustered stats ?

                    if (q.getLock() != null) {
                        state.lock = new TrackingZooKeeperGroup<QueryNodeState>(bundleContext, getGroupPath(q), QueryNodeState.class);
                        state.lock.add(new GroupListener<QueryNodeState>() {
                            @Override
                            public void groupEvent(Group<QueryNodeState> group, GroupEvent event) {
                                try {
                                    state.lock.update(new QueryNodeState(queryName, containerName,
                                            state.lock.isMaster() ? new String[] { "stat" } : null));
                                } catch (IllegalStateException e) {
                                    // not joined ? ignore
                                }
                            }
                        });
                        state.lock.update(new QueryNodeState(queryName, containerName, null));
                        state.lock.start();
                    }

                    long delay = q.getPeriod() > 0 ? q.getPeriod() : defaultDelay;
                    state.future = this.executor.scheduleAtFixedRate(
                            new Task(state),
                            Math.round(Math.random() * 1000) + 1,
                            delay * 1000,
                            TimeUnit.MILLISECONDS);
                    queries.put(q, state);
                }
            }
        } catch (RejectedExecutionException t) {
            // Ignore, the thread pool has been shut down
        } catch (Throwable t) {
            LOG.warn("Error while starting metrics", t);
        }
    }

    protected synchronized String getGroupPath(Query q) {
        if (LOCK_GLOBAL.equals(q.getLock())) {
            return "/fabric/registry/clusters/insight-metrics/global/" + q.getName();
        } else if (LOCK_HOST.equals(q.getLock())) {
            String host;
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Unable to retrieve host name", e);
            }
            return "/fabric/registry/clusters/insight-metrics/host-" + host + "/" + q.getName();
        } else {
            throw new IllegalArgumentException("Unknown lock type: " + q.getLock());
        }
    }

    protected void loadProfile(Profile profile, Set<Query> queries) {
        Map<String, byte[]> fileConfigurations = profile.getFileConfigurations();
        byte[] bytes = fileConfigurations.get(GRAPH_JSON);
        if (bytes != null && bytes.length > 0) {
            try {
                Map object = new ObjectMapper().readValue(bytes, Map.class);
                for (Map q : (List<Map>) object.get(QUERIES)) {
                    String name = (String) q.get(NAME);
                    String template = (String) q.get(TEMPLATE);
                    String metadata = (String) q.get(METADATA);
                    String lock = (String) q.get(LOCK);
                    int period = DEFAULT.equals(q.get(PERIOD)) ? defaultDelay : q.get(PERIOD) != null ? ((Number) q.get(PERIOD)).intValue() : defaultDelay;
                    int minPeriod = DEFAULT.equals(q.get(MIN_PERIOD)) ? defaultDelay : q.get(MIN_PERIOD) != null ? ((Number) q.get(MIN_PERIOD)).intValue() : period;
                    Set<Request> requests = new HashSet<Request>();
                    for (Map mb : (List<Map>) q.get(REQUESTS)) {
                        if (mb.containsKey(ATTRS)) {
                            String mname = (String) mb.get(NAME);
                            String mobj = (String) mb.get(OBJ);
                            List<String> mattrs = (List<String>) mb.get(ATTRS);
                            requests.add(new MBeanAttrs(mname, mobj, mattrs));
                        } else if (mb.containsKey(OPER)) {
                            String mname = (String) mb.get(NAME);
                            String mobj = (String) mb.get(OBJ);
                            String moper = (String) mb.get(OPER);
                            List<Object> margs = (List<Object>) mb.get(ARGS);
                            List<String> msig = (List<String>) mb.get(SIG);
                            requests.add(new MBeanOpers(mname, mobj, moper, margs, msig));
                        } else {
                            throw new IllegalArgumentException("Unknown request " + ScriptUtils.toJson(mb));
                        }
                    }
                    queries.add(new Query(name, requests, template, metadata, lock, period, minPeriod));
                }
            } catch (Throwable t) {
                LOG.warn("Unable to load queries from profile " + profile.getId(), t);
            }
        }
        for (Profile p : profile.getParents()) {
            loadProfile(p, queries);
        }
    }

    class Task implements Runnable {

        private final QueryState query;

        public Task(QueryState query) {
            this.query = query;
        }

        @Override
        public void run() {
            try {
                MBeanServer mbs = mbeanServer.getService();
                StorageService svc = storage.getService();
                // Abort if required services aren't available
                if (mbs == null || svc == null) {
                    return;
                }
                // If there's a lock, check we are the master
                if (query.lock != null && !query.lock.isMaster()) {
                    return;
                }
                QueryResult qrs = JmxUtils.execute(query.server, query.query, mbs);
                boolean forceSend = query.query.getMinPeriod() == query.query.getPeriod() ||
                        qrs.getTimestamp().getTime() - query.lastSent >= TimeUnit.SECONDS.toMillis(query.query.getMinPeriod());
                if (!forceSend && query.lastResult != null) {
                    if (qrs.getResults().equals(query.lastResult.getResults())) {
                        query.lastResult = qrs;
                        query.lastResultSent = false;
                        return;
                    }
                    if (!query.lastResultSent) {
                        renderAndSend(svc, query.lastResult);
                    }
                }
                query.lastResult = qrs;
                query.lastResultSent = true;
                query.lastSent = qrs.getTimestamp().getTime();
                renderAndSend(svc, qrs);
            } catch (Throwable e) {
                LOG.debug("Error sending metrics", e);
            }
        }

        private void renderAndSend(StorageService svc, QueryResult qrs) throws Exception {
            String output = renderer.render(qrs);
            if (output == null || output.trim().isEmpty()) {
                return;
            }
            svc.store(type + "-" + qrs.getQuery().getName(),
                    qrs.getTimestamp().getTime(),
                    output);
        }

    }

}
