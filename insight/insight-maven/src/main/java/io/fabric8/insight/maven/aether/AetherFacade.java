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
package io.fabric8.insight.maven.aether;

import java.lang.management.ManagementFactory;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyResolutionException;

public class AetherFacade implements AetherFacadeMXBean {

    private Aether aether = new Aether();

    private MBeanServer mBeanServer;
    private ObjectName objectName;

    public AetherFacade(MBeanServer mBeanServer, ObjectName objectName) {
        this.mBeanServer = mBeanServer;
        this.objectName = objectName;
    }

    public void init() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        if (objectName == null) {
            objectName = new ObjectName("org.fusesource.insight:type=Maven");
        }
        if (mBeanServer == null) {
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        mBeanServer.registerMBean(this, objectName);
    }

    public void destroy() throws MBeanRegistrationException, InstanceNotFoundException {
        if (objectName != null && mBeanServer != null) {
            mBeanServer.unregisterMBean(objectName);
        }
    }

    public AetherResult resolve(String groupId, String artifactId, String version,
                String extension, String classifier) throws DependencyCollectionException, DependencyResolutionException, ArtifactResolutionException {
        return aether.resolve(groupId, artifactId, version, extension, classifier);
    }

    public CompareResult compare(String groupId, String artifactId, String version1, String version2,
                                 String extension, String classifier) throws DependencyCollectionException, ArtifactResolutionException, DependencyResolutionException {
        return aether.compare(groupId, artifactId, version1, version2, extension, classifier);
    }

}
