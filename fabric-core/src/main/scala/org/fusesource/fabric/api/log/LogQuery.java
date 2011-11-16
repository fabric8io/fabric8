/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api.log;

import org.apache.karaf.shell.log.LruList;
import org.apache.karaf.shell.log.VmLogAppender;
import org.codehaus.jackson.map.ObjectMapper;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 */
public class LogQuery implements LogQueryMBean {
    private transient Logger logger = LoggerFactory.getLogger(LogQuery.class);

    private BundleContext bundleContext;
    private VmLogAppender appender;
    private ObjectName mbeanName;

    public void init() throws Exception {
        if (bundleContext == null) {
            return;
        }
        ServiceReference[] refs = bundleContext.getServiceReferences("org.ops4j.pax.logging.spi.PaxAppender", null);
        if (refs == null) {
            System.out.println("No pax log appenders!!!");
        } else {
            for (ServiceReference ref : refs) {
                Object service = bundleContext.getService(ref);
                if (service instanceof VmLogAppender) {
                    this.appender = (VmLogAppender) service;
                }
            }
        }
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ObjectName getMbeanName() throws MalformedObjectNameException {
        if (mbeanName == null) {
            mbeanName = new ObjectName("org.fusesource.fabric:type=LogQuery");
        }
        return mbeanName;
    }

    public void setMbeanName(ObjectName mbeanName) {
        this.mbeanName = mbeanName;
    }

    public void registerMBeanServer(MBeanServer mbeanServer) {
        try {
            ObjectName name = getMbeanName();
            ObjectInstance objectInstance = mbeanServer.registerMBean(this, name);
        } catch (Exception e) {
            logger.warn("An error occured during mbean server registration: " + e, e);
        }
    }

    public void unregisterMBeanServer(MBeanServer mbeanServer) {
        if (mbeanServer != null) {
            try {
                mbeanServer.unregisterMBean(getMbeanName());
            } catch (Exception e) {
                logger.warn("An error occured during mbean server registration: " + e, e);
            }
        }
    }

    @Override
    public String getLogEvents(int count) throws IOException {
        List<PaxLoggingEvent> answer = getLogEventList(count);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(answer);
    }

    public List<PaxLoggingEvent> getLogEventList(int count) {
        List<PaxLoggingEvent> answer = new ArrayList<PaxLoggingEvent>();
        if (appender != null) {
            LruList events = appender.getEvents();
            Iterable<PaxLoggingEvent> iterable;
            if (count > 0) {
                iterable = events.getElements(count);
            } else {
                iterable = events.getElements();
            }
            for (PaxLoggingEvent event : iterable) {
                answer.add(event);
            }
        }
        return answer;
    }
}
