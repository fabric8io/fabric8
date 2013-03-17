/*
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
package org.fusesource.insight.log.service.support;

import org.fusesource.insight.log.service.Logs;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MavenCoordinates {

    private static Map<String, String> MAVEN_COORDINATES = new ConcurrentHashMap<String, String>();

    public static void addMavenCoord(Map<String, String> properties) {
        if (properties.get("maven.coordinates") == null) {
            String mavenCoord = getMavenCoordinates(properties);
            if (mavenCoord != null && !mavenCoord.isEmpty()) {
                properties.put("maven.coordinates", mavenCoord);
            }
        }
    }

    public static String[] addMavenCoord(String[] throwable) {
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

    private static String getMavenCoordinates(Map props) {
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
                    org.fusesource.insight.log.support.MavenCoordinates.appendMavenCoordinateFromPomProperties(url.openStream(), buf);
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
