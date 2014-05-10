/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.insight.log.service;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.insight.log.LogFilter;
import io.fabric8.insight.log.LogResults;
import io.fabric8.insight.log.service.support.MavenCoordinates;
import io.fabric8.insight.log.support.LogQuerySupport;
import io.fabric8.insight.log.support.Predicate;
import org.apache.karaf.shell.log.LruList;
import org.apache.karaf.shell.log.VmLogAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link LogQueryMBean} using the embedded pax appender used by karaf
 */
public class LogQuery extends LogQuerySupport implements LogQueryMBean {
    private transient Logger LOG = LoggerFactory.getLogger(LogQuery.class);

    private BundleContext bundleContext;
    private VmLogAppender appender;
    private ServiceTracker serviceTracker;

    public LogQuery() {
        mapper.getSerializationConfig().withSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    public void init() throws Exception {
        if (bundleContext == null) {
            throw new IllegalArgumentException("No bundleContext injected!");
        }
        ServiceTrackerCustomizer customizer = null;
        serviceTracker = new ServiceTracker(bundleContext, "org.ops4j.pax.logging.spi.PaxAppender", customizer);
        serviceTracker.open();
    }

    public void destroy() throws Exception {
        if (serviceTracker != null) {
            serviceTracker.close();
            serviceTracker = null;
        }
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public String getBundleMavenCoordinates(long bundleId) {
        return MavenCoordinates.getMavenCoordinates(bundleId);
    }

    @Override
    public LogResults getLogResults(int count) throws IOException {
        LogResults events = getLogEventList(count, null);
        return events;
    }

    @Override
    public  LogResults queryLogResults(LogFilter filter) {
        Predicate<PaxLoggingEvent> predicate = Logs.createPredicate(filter);
        int count = -1;
        if (filter != null) {
            count = filter.getCount();
        }
        return getLogEventList(count, predicate);
    }

    public LogResults getLogEventList(int count, Predicate<PaxLoggingEvent> predicate) {
        LogResults answer = new LogResults();
        answer.setHost(getHostName());

        long from = Long.MAX_VALUE;
        long to = Long.MIN_VALUE;
        VmLogAppender a = getAppender();
        if (a != null) {
            LruList events = a.getEvents();
            if (events != null) {
                Iterable<PaxLoggingEvent> iterable =  events.getElements();
                if (iterable != null) {
                    int matched = 0;
                    for (PaxLoggingEvent event : iterable) {
                        long timestamp = event.getTimeStamp();
                        if (timestamp > to) {
                            to = timestamp;
                        }
                        if (timestamp < from) {
                            from = timestamp;
                        }
                        if (predicate == null || predicate.matches(event)) {
                            answer.addEvent(Logs.newInstance(event));
                            matched += 1;
                            if (count > 0 && matched >= count) {
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            LOG.warn("No VmLogAppender available!");
        }
        answer.setFromTimestamp(from);
        answer.setToTimestamp(to);
        return answer;
    }

    public VmLogAppender getAppender() {
        if (appender == null && serviceTracker != null) {
            Object[] services = serviceTracker.getServices();
            if (services != null) {
                for (Object service : services) {
                    if (service instanceof VmLogAppender) {
                        return (VmLogAppender) service;
                    }
                }
            }
        }
        return appender;
    }

    public void setAppender(VmLogAppender appender) {
        this.appender = appender;
    }
}
