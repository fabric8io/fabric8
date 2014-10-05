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
package io.fabric8.insight.log.log4j;

import io.fabric8.maven.url.ServiceConstants;
import io.fabric8.maven.url.internal.AetherBasedResolver;
import io.fabric8.maven.util.MavenConfigurationImpl;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import io.fabric8.insight.log.LogEvent;
import io.fabric8.insight.log.LogFilter;
import io.fabric8.insight.log.LogResults;
import io.fabric8.insight.log.support.LogQuerySupport;
import io.fabric8.insight.log.support.LruList;
import io.fabric8.insight.log.support.Predicate;
import org.ops4j.util.property.PropertiesPropertyResolver;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

import static io.fabric8.insight.log.support.Strings.contains;

/**
 * A log4j adapter for LogQueryMBean
 */
@Component(name = "io.fabric8.insight.log4j.Log4jLogQuery", immediate = true, metatype = false, policy = ConfigurationPolicy.IGNORE,
        label = "Fabric8 Insight Log4j LogQuery",
        description = "Provides a JMX API to query logging events")
public class Log4jLogQuery extends LogQuerySupport implements Log4jLogQueryMBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(Log4jLogQuery.class);

    private int size = 1000;
    private LruList<LoggingEvent> events;
    private boolean addMavenCoordinates = true;
    private AetherBasedResolver resolver;
    private Properties properties = new Properties();
    private MavenConfigurationImpl config;
    private Appender appender = new AppenderSkeleton() {
        protected void append(LoggingEvent loggingEvent) {
            logMessage(loggingEvent);
        }

        public void close() {
        }

        public boolean requiresLayout() {
            return true;
        }
    };


    @PostConstruct
    @Activate
    public void start() {
        super.start();

        reconnectAppender();
    }

    @Override
    public void reconnectAppender() {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        AppenderAttachable appenderAttachable = null;
        if (loggerFactory instanceof AppenderAttachable) {
            appenderAttachable = (AppenderAttachable) loggerFactory;
        }
        if (appenderAttachable == null) {
            appenderAttachable = LogManager.getRootLogger();
        }
        if (appenderAttachable != null) {
            appender.setName("LogQuery");
            appenderAttachable.addAppender(appender);
        } else {
            LOG.error("No ILoggerFactory found so cannot attach appender!");
        }
    }

    @PreDestroy
    @Deactivate
    public void stop() {
        super.stop();
    }

    public LogResults getLogResults(int maxCount) throws IOException {
        return filterLogResults(null, maxCount);
    }

    public LogResults queryLogResults(LogFilter filter) {
        Predicate<LogEvent> predicate = createPredicate(filter);
        int maxCount = -1;
        if (filter != null) {
            maxCount = filter.getCount();
        }
        return filterLogResults(predicate, maxCount);
    }

    private Predicate<LogEvent> createPredicate(LogFilter filter) {
        if (filter == null) {
            return null;
        }
        final List<Predicate<LogEvent>> predicates = new ArrayList<Predicate<LogEvent>>();

        final Set<String> levels = filter.getLevelsSet();
        if (levels.size() > 0) {
            predicates.add(new Predicate<LogEvent>() {
                @Override
                public boolean matches(LogEvent event) {
                    String level = event.getLevel();
                    return level != null && levels.contains(level.toString());
                }
            });
        }
        final Long before = filter.getBeforeTimestamp();
        if (before != null) {
            final Date date = new Date(before);
            predicates.add(new Predicate<LogEvent>() {
                @Override
                public boolean matches(LogEvent event) {
                    Date time = event.getTimestamp();
                    return time != null && time.before(date);
                }
            });
        }
        final Long after = filter.getAfterTimestamp();
        if (after != null) {
            final Date date = new Date(after);
            predicates.add(new Predicate<LogEvent>() {
                @Override
                public boolean matches(LogEvent event) {
                    Date time = event.getTimestamp();
                    return time != null && time.after(date);
                }
            });
        }

        final String matchesText = filter.getMatchesText();
        if (matchesText != null && matchesText.length() > 0) {
            predicates.add(new Predicate<LogEvent>() {
                @Override
                public boolean matches(LogEvent event) {
                    if (contains(matchesText, event.getClassName(), event.getMessage(), event.getLogger(), event.getThread())) {
                        return true;
                    }
                    String[] throwableStrRep = event.getException();
                    if (throwableStrRep != null && contains(matchesText, throwableStrRep)) {
                        return true;
                    }
                    Map properties = event.getProperties();
                    if (properties != null && contains(matchesText, properties.toString())) {
                        return true;
                    }
                    return false;
                }
            });
        }

        if (predicates.size() == 0) {
            return null;
        } else if (predicates.size() == 1) {
            return predicates.get(0);
        } else {
            return new Predicate<LogEvent>() {
                @Override
                public String toString() {
                    return "AndPredicate" + predicates;
                }

                @Override
                public boolean matches(LogEvent event) {
                    for (Predicate<LogEvent> predicate : predicates) {
                        if (!predicate.matches(event)) {
                            return false;
                        }
                    }
                    return true;
                }
            };
        }
    }

    protected LogResults filterLogResults(Predicate<LogEvent> predicate, int maxCount) {
        int matched = 0;
        long from = Long.MAX_VALUE;
        long to = Long.MIN_VALUE;
        List<LogEvent> list = new ArrayList<LogEvent>();
        Iterable<LoggingEvent> elements = getEvents().getElements();
        for (LoggingEvent element : elements) {
            LogEvent logEvent = toLogEvent(element);
            long timestamp = element.getTimeStamp();
            if (timestamp > to) {
                to = timestamp;
            }
            if (timestamp < from) {
                from = timestamp;
            }
            if (logEvent != null) {
                if (predicate == null || predicate.matches(logEvent)) {
                    list.add(logEvent);
                    matched += 1;
                    if (maxCount > 0 && matched >= maxCount) {
                        break;
                    }
                }
            }
        }
        LogResults results = new LogResults();
        results.setEvents(list);
        if (from < Long.MAX_VALUE) {
            results.setFromTimestamp(from);
        }
        if (to > Long.MIN_VALUE) {
            results.setToTimestamp(to);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Requested " + maxCount + " logging items. returning "
                    + results.getEvents().size() + " event(s) from a possible " + getEvents().size());

        }
        return results;
    }

    protected LogEvent toLogEvent(LoggingEvent element) {
        LogEvent answer = new LogEvent();
        answer.setClassName(element.getFQNOfLoggerClass());
        // TODO
        //answer.setContainerName(element.get);
        ThrowableInformation throwableInformation = element.getThrowableInformation();
        if (throwableInformation != null) {
            ThrowableFormatter renderer = new ThrowableFormatter();
            String[] stack = renderer.doRender(throwableInformation.getThrowable());
            if (stack == null) {
                stack = element.getThrowableStrRep();
            }
            answer.setException(stack);
        }
        LocationInfo locationInformation = element.getLocationInformation();
        if (locationInformation != null) {
            answer.setFileName(locationInformation.getFileName());
            answer.setClassName(locationInformation.getClassName());
            answer.setMethodName(locationInformation.getMethodName());
            answer.setLineNumber(locationInformation.getLineNumber());
        }
        Level level = element.getLevel();
        if (level != null) {
            answer.setLevel(level.toString());
        }
        // TODO
        answer.setLogger(element.getLoggerName());
        Category logger = element.getLogger();
        Object message = element.getMessage();
        if (message != null) {
            // TODO marshal differently?
            answer.setMessage(message.toString());
        }
        answer.setProperties(element.getProperties());
        // TODO
        answer.setSeq(element.getTimeStamp());
        answer.setTimestamp(new Date(element.getTimeStamp()));
        answer.setThread(element.getThreadName());
        answer.setHost(getHostName());
        return answer;
    }

    protected String filterLogEvents(LogFilter filter) throws IOException {
        // TODO
        return null;
    }


    protected void appendMavenCoordinates(LoggingEvent loggingEvent) {
        LocationInfo information = loggingEvent.getLocationInformation();
        if (information != null) {
            String coordinates = MavenCoordHelper.getMavenCoordinates(information.getClassName());
            if (coordinates != null) {
                loggingEvent.setProperty("maven.coordinates", coordinates);
            }
        }
    }

    protected String loadCoords(String coords, String filePath, String classifier) throws IOException {
        String[] split = coords.split("/");
        if (split != null && split.length > 2) {
            String groupId = split[0];
            String artifactId = split[1];
            String version = split[2];
            if (resolver == null) {
                Properties defaultProperties = getDefaultProperties();
                Properties systemProperties = System.getProperties();
                if (config == null) {
                    Properties combined = new Properties();
                    combined.putAll(defaultProperties);
                    combined.putAll(systemProperties);
                    if (properties != null) {
                        combined.putAll(properties);
                    }
                    config = new MavenConfigurationImpl(new PropertiesPropertyResolver(combined), ServiceConstants.PID);
                }
                resolver = new AetherBasedResolver(config);
            }
            File file = resolver.resolveFile(groupId, artifactId, classifier, "jar", version);
            if (file.exists() && file.isFile()) {
                if (isRoot(filePath)) {
                    return jarIndex(file);
                }
                String fileUri = file.toURI().toString();
                URL url = new URL("jar:" + fileUri + "!" + filePath);
                return loadString(url);
            }
        }
        return null;
    }

    protected Properties getDefaultProperties() {
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty("org.ops4j.pax.url.mvn.repositories",
                "http://repo1.maven.org/maven2@id=maven.central.repo, " +
                "https://repo.fusesource.com/nexus/content/repositories/releases@id=fusesource.release.repo, " +
                "https://repo.fusesource.com/nexus/content/groups/ea@id=fusesource.ea.repo, " +
                "http://svn.apache.org/repos/asf/servicemix/m2-repo@id=servicemix.repo, " +
                "http://repository.springsource.com/maven/bundles/release@id=springsource.release.repo, " +
                "http://repository.springsource.com/maven/bundles/external@id=springsource.external.repo, " +
                "https://oss.sonatype.org/content/groups/scala-tools@id=scala.repo");
        return defaultProperties;
    }

    // Properties
    //-------------------------------------------------------------------------
    public LruList<LoggingEvent> getEvents() {
        if (events == null) {
            events = new LruList<LoggingEvent>(LoggingEvent.class, getSize());
        }
        return events;
    }

    public void setEvents(LruList<LoggingEvent> events) {
        this.events = events;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isAddMavenCoordinates() {
        return addMavenCoordinates;
    }

    public void setAddMavenCoordinates(boolean addMavenCoordinates) {
        this.addMavenCoordinates = addMavenCoordinates;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public MavenConfigurationImpl getConfig() {
        return config;
    }

    public void setConfig(MavenConfigurationImpl config) {
        this.config = config;
    }

    public AetherBasedResolver getResolver() {
        return resolver;
    }

    public void setResolver(AetherBasedResolver resolver) {
        this.resolver = resolver;
    }

	@Override
	public void logMessage(LoggingEvent record) {
        if (addMavenCoordinates) {
            appendMavenCoordinates(record);
        }
		getEvents().add(record);
	}
}
