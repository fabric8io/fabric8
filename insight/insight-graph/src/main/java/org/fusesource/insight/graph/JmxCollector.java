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

package org.fusesource.insight.graph;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServer;

import com.googlecode.jmxtrans.OutputWriter;
import com.googlecode.jmxtrans.jobs.ServerJob;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.ValidationException;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.zookeeper.data.ACL;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonMappingException;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.insight.graph.support.Json;
import org.fusesource.insight.graph.support.SchedulerFactory;
import org.fusesource.insight.graph.support.ZKClusterOutputWriter;
import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects all the charting statistics defined against its profiles
 */
public class JmxCollector {
    public static final String GRAPH_JSON = "org.fusesource.insight.graph.json";
    public static int SECONDS_BETWEEN_SERVER_JOB_RUNS = 60;
    public static final String GRAPH_CLUSTER_PREFIX = "graphCluster.";

    private static final transient Logger LOG = LoggerFactory.getLogger(JmxCollector.class);
    protected static final String DEFAULT_GRAPH_CLUSTER_NAME = "default";

    private final FabricService fabricService;
    private List<Server> masterServersList = new ArrayList<Server>();
    private Map<String, KeyedObjectPool> objectPoolMap;
    private Scheduler scheduler;
    private MBeanServer mbeanServer;
    private IZKClient zkClient;
    private String clusterRoot = "/fabric/registry/insight/stats";
    private Map<String,ZKClusterOutputWriter> outputWriters = new HashMap<String, ZKClusterOutputWriter>();

    public JmxCollector(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public void start() throws IOException, ValidationException, LifecycleException, SchedulerException, ParseException {
        Container container = fabricService.getCurrentContainer();
        if (container != null) {
            Profile[] profiles = container.getProfiles();
            if (profiles != null) {
                for (Profile profile : profiles) {
                    loadProfile(container, profile);
                }
            }
        }
        process();
    }

    public void destroy() throws Exception {
        for (ZKClusterOutputWriter outputWriter : outputWriters.values()) {
            if (outputWriter != null) {
                outputWriter.stop();
            }
        }
    }


    public void registerMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    public void unregisterMBeanServer(MBeanServer mbeanServer) {
    }

    public Scheduler getScheduler() throws SchedulerException {
        if (scheduler == null) {
            LOG.warn("No scheduler configured so creating a default implementation");
            scheduler = new SchedulerFactory().createScheduler();
        }
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public IZKClient getZkClient() {
        return zkClient;
    }

    public void setZkClient(IZKClient zkClient) {
        this.zkClient = zkClient;
    }

    public String getClusterRoot() {
        return clusterRoot;
    }

    public void setClusterRoot(String clusterRoot) {
        this.clusterRoot = clusterRoot;
    }

    public void process() throws LifecycleException, ValidationException, SchedulerException, ParseException {
        for (Server server : this.masterServersList) {
            if (server.isLocal()) {
                server.setLocalMBeanServer(mbeanServer);
                server.setAlias(fabricService.getCurrentContainerName());
            }
            // need to inject the poolMap
            for (Query query : server.getQueries()) {
                query.setServer(server);

                for (OutputWriter writer : query.getOutputWriters()) {
                    writer.setObjectPoolMap(getObjectPoolMap());
                    writer.start();
                }
            }

            // Now validate the setup of each of the OutputWriter's per query.
            validateSetup(server.getQueries());

            // Now schedule the jobs for execution.
            scheduleJob(server);
        }

    }

    private void scheduleJob(Server server) throws SchedulerException, ParseException {
        Scheduler scheduler = getScheduler();
        String name = server.getHost() + ":" + server.getPort() + "-" + System.currentTimeMillis();
        JobDetail jd = new JobDetail(name, "ServerJob", ServerJob.class);

        JobDataMap map = new JobDataMap();
        map.put(Server.class.getName(), server);
        map.put(Server.JMX_CONNECTION_FACTORY_POOL, this.getObjectPoolMap().get(Server.JMX_CONNECTION_FACTORY_POOL));
        jd.setJobDataMap(map);

        Trigger trigger = null;

        if ((server.getCronExpression() != null) && CronExpression.isValidExpression(server.getCronExpression())) {
            trigger = new CronTrigger();
            ((CronTrigger) trigger).setCronExpression(server.getCronExpression());
            ((CronTrigger) trigger).setName(server.getHost() + ":" + server.getPort() + "-" + Long.valueOf(System.currentTimeMillis()).toString());
            ((CronTrigger) trigger).setStartTime(new Date());
        } else {
            Trigger minuteTrigger = TriggerUtils.makeSecondlyTrigger(SECONDS_BETWEEN_SERVER_JOB_RUNS);
            minuteTrigger.setName(server.getHost() + ":" + server.getPort() + "-" + Long.valueOf(System.currentTimeMillis()).toString());
            minuteTrigger.setStartTime(new Date());

            trigger = minuteTrigger;
        }

        scheduler.scheduleJob(jd, trigger);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scheduled job: " + jd.getName() + " for server: " + server);
        }
    }

    private void validateSetup(List<Query> queries) throws ValidationException {
        for (Query q : queries) {
            this.validateSetup(q);
        }
    }

    private void validateSetup(Query query) throws ValidationException {
        List<OutputWriter> writers = query.getOutputWriters();
        if (writers != null) {
            for (OutputWriter w : writers) {
                w.validateSetup(query);
            }
        }
    }


    /**
     * Returns the object pool map
     */
    @JsonIgnore
    public Map<String, KeyedObjectPool> getObjectPoolMap() {
        if (this.objectPoolMap == null) {
            this.objectPoolMap = JmxUtils.getDefaultPoolMap();
        }
        return this.objectPoolMap;
    }

    public void setObjectPoolMap(Map<String, KeyedObjectPool> poolMap) {
        this.objectPoolMap = poolMap;
    }

    protected void loadProfile(Container container, Profile profile) throws IOException, LifecycleException {
        Map<String, byte[]> fileConfigurations = profile.getFileConfigurations();
        byte[] bytes = fileConfigurations.get(GRAPH_JSON);
        if (bytes != null && bytes.length > 0) {
            JmxProcess process = getJmxProcess(GRAPH_JSON, new ByteArrayInputStream(bytes));
            if (process != null) {
                List<Server> servers = process.getServers();
                for (Server server : servers) {
                    configureProfileServer(server, container, profile);
                }
                JmxUtils.mergeServerLists(this.masterServersList, servers);
            }
        }
        for (Profile p : profile.getParents()) {
            loadProfile(container, p);
        }
    }

    protected void configureProfileServer(Server server, Container container, Profile profile) throws LifecycleException {
        if (server.isLocal()) {
            server.setLocalMBeanServer(mbeanServer);

            // TODO we could maybe customize this on a per profile basis
            // e.g. you may wish to look at generic JVM stats for just all the brokers
            String serverAlias = profile.getId() + "." + fabricService.getCurrentContainerName();
            server.setAlias(serverAlias);
        }
        List<Query> queries = server.getQueries();
        for (Query query : queries) {
            List<OutputWriter> writers = query.getOutputWriters();
            if (writers == null) {
                writers = new ArrayList<OutputWriter>();
                query.setOutputWriters(writers);
            }
            if (writers.isEmpty()) {
                // lets find the graph writer clusters for the profile
                Map<String, String> containerProperties = profile.getContainerConfiguration();

                // now lets find all the graph profiles
                Set<String> clusterNames = new HashSet<String>();
                for (Map.Entry<String, String> entry : containerProperties.entrySet()) {
                    String key = entry.getKey();
                    String clusterName = entry.getValue();

                    if (key.startsWith(GRAPH_CLUSTER_PREFIX)) {
                        clusterNames.add(clusterName);
                    }
                }
                if (clusterNames.isEmpty()) {
                    clusterNames.add(DEFAULT_GRAPH_CLUSTER_NAME);
                }

                for (String clusterName : clusterNames) {
                    OutputWriter writer = createClusterWriter(clusterName);
                    if (writer != null) {
                        writers.add(writer);
                    }
                }
            }
        }
    }

    /**
     * Lets look in ZK and see what the definition of the graphing cluster is and create an OutputWriter for that cluster
     */
    protected OutputWriter createClusterWriter(String clusterName) throws LifecycleException {
        ZKClusterOutputWriter outputWriter = outputWriters.get(clusterName);
        if (outputWriter == null) {
            String zkPath = getClusterRoot() + "/" + clusterName;
            outputWriter = new ZKClusterOutputWriter(this, zkPath);
            outputWriters.put(clusterName, outputWriter);
            outputWriter.start();
        }
        return outputWriter;
    }

    public static JmxProcess getJmxProcess(String name, InputStream in) throws JsonParseException, JsonMappingException, IOException {
        JmxProcess jmx = Json.readJsonValue(name, in, JmxProcess.class);
        jmx.setName(name);
        return jmx;
    }

}
