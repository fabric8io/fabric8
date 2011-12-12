/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.insight.graph;

import com.googlecode.jmxtrans.OutputWriter;
import com.googlecode.jmxtrans.jobs.ServerJob;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.ValidationException;
import org.apache.commons.pool.KeyedObjectPool;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
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

import javax.management.MBeanServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Collects all the charting statistics defined against its profiles
 */
public class JmxCollector {
    public static final String GRAPH_JSON = "org.fusesource.insight.graph.json";
    public static int SECONDS_BETWEEN_SERVER_JOB_RUNS = 60;

    private static final transient Logger LOG = LoggerFactory.getLogger(JmxCollector.class);

    private final FabricService fabricService;
    private List<Server> masterServersList = new ArrayList<Server>();
    private Map<String, KeyedObjectPool> objectPoolMap;
    private Scheduler scheduler;

    public JmxCollector(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public void start() throws IOException, ValidationException, LifecycleException, SchedulerException, ParseException {
        Agent agent = fabricService.getCurrentAgent();
        if (agent != null) {
            Profile[] profiles = agent.getProfiles();
            if (profiles != null) {
                for (Profile profile : profiles) {
                    loadProfile(agent, profile);
                }
            }
        }
        process();
    }

    public void destroy() throws Exception {

    }

    
    public void registerMBeanServer(MBeanServer mbeanServer) {
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

    public void process() throws LifecycleException, ValidationException, SchedulerException, ParseException {
        for (Server server : this.masterServersList) {
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
      			((CronTrigger)trigger).setCronExpression(server.getCronExpression());
      			((CronTrigger)trigger).setName(server.getHost() + ":" + server.getPort() + "-" + Long.valueOf(System.currentTimeMillis()).toString());
      			((CronTrigger)trigger).setStartTime(new Date());
      		} else {
      			Trigger minuteTrigger = TriggerUtils.makeSecondlyTrigger(SECONDS_BETWEEN_SERVER_JOB_RUNS);
      			minuteTrigger.setName(server.getHost() + ":" + server.getPort() + "-" + Long.valueOf(System.currentTimeMillis()).toString());
      			minuteTrigger.setStartTime(new Date());

      			trigger = minuteTrigger;
      		}

      		this.scheduler.scheduleJob(jd, trigger);
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

    protected void loadProfile(Agent agent, Profile profile) throws IOException {
        Map<String, byte[]> fileConfigurations = profile.getFileConfigurations();
        byte[] bytes = fileConfigurations.get(GRAPH_JSON);
        if (bytes != null && bytes.length > 0) {
            JmxProcess process = JmxUtils.getJmxProcess(GRAPH_JSON, new ByteArrayInputStream(bytes));
            if (process != null) {
                JmxUtils.mergeServerLists(this.masterServersList, process.getServers());
            }
        }
    }
}
