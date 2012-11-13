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
package org.fusesource.insight.log.support;

import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.insight.log.LogFilter;
import org.fusesource.insight.log.LogResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Base class for any {@link org.fusesource.insight.log.service.LogQueryMBean} implementation
 */
public abstract class LogQuerySupport implements LogQuerySupportMBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(LogQuerySupport.class);

    protected ObjectMapper mapper = new ObjectMapper();
    private ObjectName mbeanName;
    private MBeanServer mbeanServer;
    private String hostName;

    protected LogQuerySupport() {
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.warn("Failed to get host name: " + e, e);
        }
    }

    /**
     * Registers the object with JMX
     */
    public void start() {
        MBeanServer server = getMbeanServer();
        if (server != null) {
            registerMBeanServer(server);
        } else {
            LOG.error("No MBeanServer available so cannot register mbean");
        }
    }

    /**
     * Unregisters the object with JMX
     */
    public void stop() {
        MBeanServer server = getMbeanServer();
        if (server != null) {
            unregisterMBeanServer(server);
        }
    }

    public LogResults allLogResults() throws IOException {
        return getLogResults(-1);
    }

    public String getLogEvents(int maxCount) throws IOException {
        LogResults results = getLogResults(maxCount);
        return toJSON(results);
    }

    @Override
    public String filterLogEvents(String jsonFilter) throws IOException {
        LogResults results = jsonQueryLogResults(jsonFilter);
        return toJSON(results);
    }

    @Override
    public LogResults jsonQueryLogResults(String jsonFilter) throws IOException {
        LogFilter filter = jsonToLogFilter(jsonFilter);
        return queryLogResults(filter);
    }

    public ObjectName getMbeanName() throws MalformedObjectNameException {
        if (mbeanName == null) {
            mbeanName = new ObjectName("org.fusesource.insight:type=LogQuery");
        }
        return mbeanName;
    }

    public void setMbeanName(ObjectName mbeanName) {
        this.mbeanName = mbeanName;
    }

    public MBeanServer getMbeanServer() {
        if (mbeanServer == null) {
            mbeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        return mbeanServer;
    }

    public void setMbeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void registerMBeanServer(MBeanServer mbeanServer) {
        try {
            ObjectName name = getMbeanName();
            ObjectInstance objectInstance = mbeanServer.registerMBean(this, name);
        } catch (Exception e) {
            LOG.warn("An error occured during mbean server registration: " + e, e);
        }
    }

    public void unregisterMBeanServer(MBeanServer mbeanServer) {
        if (mbeanServer != null) {
            try {
                mbeanServer.unregisterMBean(getMbeanName());
            } catch (Exception e) {
                LOG.warn("An error occured during mbean server registration: " + e, e);
            }
        }
    }

    protected String toJSON(Object answer) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, answer);
            return writer.toString();
        } catch (IOException e) {
            LOG.warn("Failed to marshal the events: " + e, e);
            throw new IOException(e.getMessage());
        }
    }

    protected LogFilter jsonToLogFilter(String json) throws IOException {
        if (json == null) {
            return null;
        }
        json = json.trim();
        if (json.length() == 0 || json.equals("{}")) {
            return null;
        }
        return mapper.reader(LogFilter.class).readValue(json);
    }

}
