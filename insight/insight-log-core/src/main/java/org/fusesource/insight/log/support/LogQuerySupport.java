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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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

    protected static String loadString(URL url) throws IOException {
        InputStream is = url.openStream();
        if (is == null) {
            return null;
        }
        try {
            InputStreamReader reader = new InputStreamReader(is);
            StringWriter writer = new StringWriter();
            final char[] buffer = new char[4096];
            int n;
            while ( -1 != ( n = reader.read( buffer ) ) )
            {
                writer.write( buffer, 0, n );
            }
            writer.flush();
            return writer.toString();
        } finally {
            is.close();
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

    @Override
    public LogResults logResultsSince(long time) throws IOException {
        LogFilter filter = new LogFilter();
        filter.setAfterTimestamp(time);
        return queryLogResults(filter);
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

    public String getSource(String mavenCoords, String className, String filePath) throws IOException {
        // the fileName could be just a name and extension so we may have to use the className to make a fully qualified package
        String classNamePath = null;
        if (!Strings.isEmpty(className)) {
            classNamePath = className.replace('.', '/') + ".java";
        }
        if (Strings.isEmpty(filePath)) {
            filePath = classNamePath;
        } else {
            // we may have package in the className but not in the file name
            if (filePath.lastIndexOf('/') <= 0 && classNamePath != null) {
                int idx = classNamePath.lastIndexOf('/');
                if (idx > 0) {
                    filePath = classNamePath.substring(0, idx) + ensureStartsWithSlash(filePath);
                }
            }
        }
        return getArtifactFile(mavenCoords, filePath, "sources");
    }

    public String getJavaDoc(String mavenCoordinates, String filePath) throws IOException {
        return getArtifactFile(mavenCoordinates, filePath, "javadoc");
    }

    protected String getArtifactFile(String mavenCoords, String filePath, String classifier) throws IOException {
        filePath = ensureStartsWithSlash(filePath);

        String coords = mavenCoords.replace(':', '/');
        String[] array = coords.split("\\s+");
        if (array == null || array.length < 2) {
            return loadCoords(coords, filePath, classifier);
        } else {
            // lets enumerate all values if space separated
            if (isRoot(filePath)) {
                StringBuilder buffer = new StringBuilder();
                for (String coord : array) {
                    try {
                        String text = loadCoords(coord, filePath, classifier);
                        if (text != null) {
                            buffer.append(text);
                        }
                    } catch (IOException e) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("" + e);
                        }
                    }
                }
                return buffer.toString();
            } else {
                for (String coord : array) {
                    try {
                        return loadCoords(coord, filePath, classifier);
                    } catch (IOException e) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("" + e);
                        }
                    }
                }
                return null;
            }
        }
    }

    protected String loadCoords(String coords, String filePath, String classifier) throws IOException {
        URL url = new URL("jar:mvn:" + coords + "/jar/" + classifier + "!" + filePath);
        if (isRoot(filePath)) {
            return jarIndex(url);
        }
        return loadString(url);
    }

    protected String jarIndex(URL url) throws IOException {
        StringBuilder buffer = new StringBuilder();
        JarURLConnection uc = (JarURLConnection) url.openConnection();
        return jarIndex(uc.getJarFile());
    }

    protected String jarIndex(File file) throws IOException {
        JarFile jarFile = new JarFile(file);
        return jarIndex(jarFile);
    }

    protected String jarIndex(JarFile jarFile) {
        StringBuilder buffer = new StringBuilder();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            addJarEntryToIndex(entry, buffer);
        }
        return buffer.toString();
    }

    protected void addJarEntryToIndex(JarEntry entry, StringBuilder buffer) {
        // no need to show empty directories
        if (!entry.isDirectory()) {
            buffer.append(entry.getName());
            buffer.append("\n");
        }
    }



    /**
     * Returns true if the file path is "/" or empty
     */
    protected boolean isRoot(String filePath) {
        return filePath == null || filePath.length() == 0 || filePath.equals("/");
    }

    public static String ensureStartsWithSlash(String path) {
        if (path != null && !path.startsWith("/")) {
            return "/" + path;
        } else {
            return path;
        }
    }

}
