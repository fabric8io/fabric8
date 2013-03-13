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

package org.fusesource.insight.log.service;

import org.fusesource.insight.log.LogEvent;
import org.fusesource.insight.log.LogFilter;
import org.fusesource.insight.log.support.MavenCoordinates;
import org.fusesource.insight.log.support.Predicate;
import org.fusesource.insight.log.support.Strings;
import org.ops4j.pax.logging.spi.PaxLevel;
import org.ops4j.pax.logging.spi.PaxLocationInfo;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class Logs {
    public static LogEvent newInstance(PaxLoggingEvent event) {
        LogEvent answer = new LogEvent();
        answer.setLevel(toString(event.getLevel()));
        answer.setMessage(event.getMessage());
        answer.setLogger(event.getLoggerName());
        answer.setTimestamp(new Date(event.getTimeStamp()));
        answer.setThread(event.getThreadName());
        answer.setException(addMavenCoord(event.getThrowableStrRep()));

        Map eventProperties = event.getProperties();
        if (eventProperties != null && eventProperties.size() > 0) {
            Map<String, String> properties = new HashMap<String, String>();
            Set<Map.Entry> set = eventProperties.entrySet();
            for (Map.Entry entry : set) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key != null && value != null) {
                    properties.put(toString(key), toString(value));
                }
            }
            if (properties.get("maven.coordinates") == null) {
                String mavenCoord = getMavenCoordinates(event);
                if (mavenCoord != null && !mavenCoord.isEmpty()) {
                    properties.put("maven.coordinates", mavenCoord);
                }
            }
            answer.setProperties(properties);
        }

        // TODO missing fields...
        // event.getRenderedMessage()
        // event.getFQNOfLoggerClass()
        PaxLocationInfo locationInformation = event.getLocationInformation();
        if (locationInformation != null) {
            answer.setClassName(locationInformation.getClassName());
            answer.setFileName(locationInformation.getFileName());
            answer.setMethodName(locationInformation.getMethodName());
            answer.setLineNumber(locationInformation.getLineNumber());
        }
        return answer;
    }

    protected static String toString(Object value) {
        // TODO should we add any custom marshalling for types?
        return value.toString();
    }

    public static Predicate<PaxLoggingEvent> createPredicate(final LogFilter filter) {
        if (filter == null) {
            return null;
        }
        final List<Predicate<PaxLoggingEvent>> predicates = new ArrayList<Predicate<PaxLoggingEvent>>();

        final Set<String> levels = filter.getLevelsSet();
        if (levels.size() > 0) {
            predicates.add(new Predicate<PaxLoggingEvent>() {
                @Override
                public boolean matches(PaxLoggingEvent event) {
                    PaxLevel level = event.getLevel();
                    return level != null && levels.contains(level.toString());
                }
            });
        }
        final Long before = filter.getBeforeTimestamp();
        if (before != null) {
            predicates.add(new Predicate<PaxLoggingEvent>() {
                @Override
                public boolean matches(PaxLoggingEvent event) {
                    long time = event.getTimeStamp();
                    return time < before;
                }
            });
        }
        final Long after = filter.getAfterTimestamp();
        if (after != null) {
            predicates.add(new Predicate<PaxLoggingEvent>() {
                @Override
                public boolean matches(PaxLoggingEvent event) {
                    long time = event.getTimeStamp();
                    return time > after;
                }
            });
        }

        final String matchesText = filter.getMatchesText();
        if (matchesText != null && matchesText.length() > 0) {
            predicates.add(new Predicate<PaxLoggingEvent>() {
                @Override
                public boolean matches(PaxLoggingEvent event) {
                    if (Strings.contains(matchesText,
                            event.getFQNOfLoggerClass(), event.getMessage(),
                            event.getLoggerName(), event.getThreadName())) {
                        return true;
                    }
                    String[] throwableStrRep = event.getThrowableStrRep();
                    if (throwableStrRep != null && Strings.contains(matchesText, throwableStrRep)) {
                        return true;
                    }
                    Map properties = event.getProperties();
                    if (properties != null && Strings.contains(matchesText, properties.toString())) {
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
            return new Predicate<PaxLoggingEvent>() {
                @Override
                public String toString() {
                    return "AndPredicate" + predicates;
                }

                @Override
                public boolean matches(PaxLoggingEvent event) {
                    for (Predicate<PaxLoggingEvent> predicate : predicates) {
                        if (!predicate.matches(event)) {
                            return false;
                        }
                    }
                    return true;
                }
            };
        }
    }

    private static Map<String, String> MAVEN_COORDINATES = new ConcurrentHashMap<String, String>();

    private static String[] addMavenCoord(String[] throwable) {
        if (throwable != null) {
            String[] newThrowable = new String[throwable.length];
            for (int i = 0; i < newThrowable.length; i++) {
                newThrowable[i] = addMavenCoord(throwable[i]);
            }
            return newThrowable;
        }
        return throwable;
    }

    private static String addMavenCoord(String line) {
        if (line.endsWith("]")) {
            int index = line.lastIndexOf('[');
            if (index > 0) {
                String str = line.substring(index + 1, line.length() - 1);
                index = str.indexOf(':');
                if (index > 0) {
                    String idStr = str.substring(0, index);
                    String mvn = getMavenCoordinates(idStr);
                    if (mvn != null) {
                        return line + "[" + mvn + "]";
                    }
                }
            }
        }
        return line;
    }

    private static String getMavenCoordinates(PaxLoggingEvent event) {
        Map props = event.getProperties();
        Object id = (props != null) ? props.get("bundle.id") : null;
        if (id == null) {
            return null;
        }
        return getMavenCoordinates(id.toString());
    }

    private static String getMavenCoordinates(String bundleIdStr) {
        long bundleId;
        try {
            bundleId = Long.parseLong(bundleIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
        return getMavenCoordinates(bundleId);
    }

    private static String getMavenCoordinates(long bundleId) {
        Bundle m_bundle = FrameworkUtil.getBundle(Logs.class).getBundleContext().getBundle(bundleId);
        if (m_bundle == null) {
            // Not sure why can't we find the bundleId?
            return null;
        }
        String id = Long.toString(m_bundle.getBundleId()) + ":" + Long.toString(m_bundle.getLastModified());
        String maven = MAVEN_COORDINATES.get(id);
        if (maven == null) {
            try {
                Enumeration<URL> e = m_bundle.findEntries("META-INF/maven/", "pom.properties", true);
                StringBuilder buf = new StringBuilder();
                while (e != null && e.hasMoreElements()) {
                    URL url = e.nextElement();
                    MavenCoordinates.appendMavenCoordinateFromPomProperties(url.openStream(), buf);
                }
                maven = buf.toString();
            } catch (Throwable t) {
                // Ignore
                maven = "";
            }
            MAVEN_COORDINATES.put(id, maven);
        }
        return maven;
    }

}
