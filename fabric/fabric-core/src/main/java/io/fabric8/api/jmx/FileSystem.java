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
package io.fabric8.api.jmx;

import java.io.File;
import java.io.IOException;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import io.fabric8.api.RuntimeProperties;
import io.fabric8.utils.SystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class FileSystem implements FileSystemMBean {

    private static final transient Logger LOG = LoggerFactory.getLogger(FileSystem.class);

    private ObjectName objectName;
    private final File fs;
    private final String path;

    public FileSystem(RuntimeProperties sysprops) {
        fs = new File(sysprops.getProperty(SystemProperties.KARAF_DATA, "karaf-data"));
        String path;
        try {
            path = fs.getCanonicalPath();
        } catch (IOException e) {
            path = fs.getAbsolutePath();
        }
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public long getTotalSpace() {
        return fs.getTotalSpace();
    }

    @Override
    public long getFreeSpace() {
        return fs.getFreeSpace();
    }

    @Override
    public long getUsableSpace() {
        return fs.getUsableSpace();
    }

    @Override
    public short getUsedPercentage() {
        long total = getTotalSpace();
        long free = getFreeSpace();
        return  (short) ((total - free) * 100 / total);
    }

    public ObjectName getObjectName() throws MalformedObjectNameException {
        if (objectName == null) {
            // TODO to avoid mbean clashes if ever a JVM had multiple FabricService instances, we may
            // want to add a parameter of the fabric ID here...
            objectName = new ObjectName("io.fabric8:service=FileSystem");
        }
        return objectName;
    }

    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    public void registerMBeanServer(MBeanServer mbeanServer) {
        try {
            ObjectName name = getObjectName();
            if (!mbeanServer.isRegistered(name)) {
                mbeanServer.registerMBean(this, name);
            }
        } catch (Exception e) {
            LOG.warn("An error occured during mbean server registration: " + e, e);
        }
    }

    public void unregisterMBeanServer(MBeanServer mbeanServer) {
        if (mbeanServer != null) {
            try {
                ObjectName name = getObjectName();
                if (mbeanServer.isRegistered(name)) {
                    mbeanServer.unregisterMBean(name);
                }
            } catch (Exception e) {
                LOG.warn("An error occured during mbean server registration: " + e, e);
            }
        }
    }
}
