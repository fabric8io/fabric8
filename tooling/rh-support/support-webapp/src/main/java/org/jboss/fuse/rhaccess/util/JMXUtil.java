/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package org.jboss.fuse.rhaccess.util;

import io.fabric8.api.CreateChildContainerMetadata;
import io.fabric8.api.CreateChildContainerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;

/**
 * Helper class to collect all the interaction for the support module with JMX
 */
public class JMXUtil {

    private final static Logger log = LoggerFactory.getLogger(JMXUtil.class);
    public static final String FABRIC_OBJECT_NAME = "io.fabric8:type=Fabric";


    //private MBeanServerConnection mBeanServer;
    private MBeanServer mBeanServer;

    public JMXUtil(MBeanServer mBeanServer) {
        HashMap<String, String[]> env = new HashMap<String, String[]>();
//        String[] credentials = new String[]{"admin", "admin"};
//        env.put("jmx.remote.credentials", credentials);
        try {
            this.mBeanServer = mBeanServer;
        } catch (Exception e) {
            throw new RuntimeException("Unable to get an MBeanServer connection", e);
        }
    }


    protected Set<ObjectName> findObjectNames(QueryExp queryExp, String... objectNames)
            throws MalformedObjectNameException, IOException {
        Set<ObjectName> answer = new HashSet<ObjectName>();
        for (String objectName : objectNames) {
            log.debug("Searching JMX for objectName [{}] with queryExp [{}]", objectName, queryExp);
            Set<ObjectName> set = mBeanServer.queryNames(new ObjectName(objectName), queryExp);
            log.info("Found {} matches", set.size());

            if (set != null) {
                answer.addAll(set);
            }
        }
        return answer;
    }

    public String[] listContainerNames() {

        String[] result = new String[0];

        String objectNames = FABRIC_OBJECT_NAME;
        String methodName = "containerIds";

        Object o = null;
        try {
            Set<ObjectName> set = findObjectNames(null, objectNames);
            if (set.size() == 0) {
                // we do not have a fabric created
                result = new String[1];
                result[0] = System.getProperty("karaf.name");
            } else {
                ObjectName objectName = (ObjectName) set.toArray()[0];
                o = mBeanServer.invoke(
                        objectName /* instance */,
                        methodName /*operationName*/,
                        new Object[]{} /*params*/,
                        new String[]{} /*signature*/);
                result = (String[]) o;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error invoking MBean operation", e);
        }

        return result;
    }


    public String[] listLocalContainerNames() {

        List<String> result = new ArrayList<String>();

        String objectNames = FABRIC_OBJECT_NAME;
        String methodName = "containerIds";

        Object o = null;
        try {
            Set<ObjectName> set = findObjectNames(null, objectNames);
            if (set.size() == 0) {
                // we do not have a fabric created
                result.add(System.getProperty("karaf.name"));
            } else {
                ObjectName objectName = (ObjectName) set.toArray()[0];
                o = mBeanServer.invoke(
                        objectName /* instance */,
                        methodName /*operationName*/,
                        new Object[]{} /*params*/,
                        new String[]{} /*signature*/);
                String[] partial = (String[]) o;

                for (String container : partial) {
                    if (isLocalChild(container)) {
                        result.add(container);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error invoking MBean operation", e);
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * Determines if a container is a child of another one
     *
     * @param containerId
     * @return
     * @throws MalformedObjectNameException
     */
    public boolean isLocalChild(String containerId) {
        boolean result = false;
        ObjectName name = null;
        try {
            name = new ObjectName(FABRIC_OBJECT_NAME);
        } catch (MalformedObjectNameException e) {
            throw new UnsupportedOperationException("Unable to query Fabric service", e);
        }

        try {
            if (!mBeanServer.isRegistered(name)) {
                throw new UnsupportedOperationException("Unable to query Fabric service");
            }

            Object o = mBeanServer.invoke(
                    name /* instance */,
                    "containers" /*operationName*/,
                    new Object[]{} /*params*/,
                    new String[]{} /*signature*/);

            List<Map<String, Object>> containerIds = (List<Map<String, Object>>) o;


            for (Map container : containerIds) {

                // supports only karaf containers atm
                if (containerId.equals(container.get("id")) && "karaf".equals(container.get("type"))) {
                    // check for a root container
                    if (container.get("metadata") == null) {
                        result = true;
                        break;
                    } else if (container.get("parentId") != null) {
                        Object ob = container.get("metadata");
                        CreateChildContainerMetadata metadata = (CreateChildContainerMetadata) ob;
                        if (metadata != null) {
                            CreateChildContainerOptions createOptions = metadata.getCreateOptions();
                            if (createOptions != null) {
                                if ("child".equals(createOptions.getProviderType())) {
                                    result = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new UnsupportedOperationException("Unable to query Fabric service", e);
        }
        return result;
    }

    public boolean isRootContainer(String containerId) {
        boolean result = false;
        ObjectName name = null;
        try {
            name = new ObjectName(FABRIC_OBJECT_NAME);
        } catch (MalformedObjectNameException e) {
            throw new UnsupportedOperationException("Unable to query Fabric service", e);
        }

        try {
            if (!mBeanServer.isRegistered(name)) {
                log.info("io.fabric8:type=Fabric MBean not found. Assume we are not member of a Fabric" );
                return true;
            }
            Object o = mBeanServer.invoke(
                    name /* instance */,
                    "containers" /*operationName*/,
                    new Object[]{} /*params*/,
                    new String[]{} /*signature*/);

            List<Map> l = (List<Map>) o;
            for (Map container : l) {
                // supports only karaf containers atm
                if (containerId.equals(container.get("id")) && "karaf".equals(container.get("type"))) {
                    result = (boolean) container.get("root");
                    break;
                }
            }
        } catch (Exception e) {
            throw new UnsupportedOperationException("Unable to query Fabric service", e);
        }
        return result;
    }

    public File takeHeapDump() throws MalformedObjectNameException, IOException, MBeanException, ReflectionException {
        ObjectName name = new ObjectName("com.sun.management:type=HotSpotDiagnostic");
        //File f = new File(System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "HeapDump.hprof");
        File f = File.createTempFile("HeapDump", ".hprof");
        //dumpHeap fails if file already exists
        f.delete();

        if (!mBeanServer.isRegistered(name)) {
            throw new UnsupportedOperationException("Unable to take HeapDump");
        }
        try {
            Object o = mBeanServer.invoke(
                    name /* instance */,
                    "dumpHeap" /*operationName*/,
                    new Object[]{f.getAbsolutePath(), Boolean.FALSE} /*params*/,
                    new String[]{String.class.getName(), boolean.class.getName()} /*signature*/);
        } catch (Exception e) {
            throw new UnsupportedOperationException("Unable to take HeapDump", e);
        }
        log.info("HeapDump created at: {}, size: {} bytes", f.getAbsolutePath(), f.length());
        log.info("Compressing HeapDump and removing uncompressed");
        File zip = File.createTempFile("HeapDump", ".hprof.zip");
        FileUtil.zipSingleFile(f, zip);
        f.delete();
        log.info("Zipped HeapDump created at: {}, size: {} bytes", zip.getAbsolutePath(), zip.length());

        return zip;
    }

    public String takeThreadDump() {
        StringBuilder dump = new StringBuilder();
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
        for (ThreadInfo threadInfo : threadInfos) {
            dump.append('"');
            dump.append(threadInfo.getThreadName());
            dump.append("\" ");
            final Thread.State state = threadInfo.getThreadState();
            dump.append("\n   java.lang.Thread.State: ");
            dump.append(state);
            final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
            for (final StackTraceElement stackTraceElement : stackTraceElements) {
                dump.append("\n        at ");
                dump.append(stackTraceElement);
            }
            dump.append("\n\n");
        }

        return dump.toString();
    }


}
