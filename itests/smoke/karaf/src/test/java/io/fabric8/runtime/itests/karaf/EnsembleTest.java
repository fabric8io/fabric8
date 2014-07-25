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
package io.fabric8.runtime.itests.karaf;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ZooKeeperClusterService;
import io.fabric8.runtime.itests.support.CommandSupport;
import io.fabric8.runtime.itests.support.ContainerBuilder;
import io.fabric8.runtime.itests.support.FabricEnsembleSupport;
import io.fabric8.runtime.itests.support.Provision;
import io.fabric8.runtime.itests.support.ServiceProxy;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.gravia.Constants;
import org.jboss.gravia.itests.support.AnnotatedContextListener;
import org.jboss.gravia.itests.support.ArchiveBuilder;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(Arquillian.class)
public class EnsembleTest {

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final ArchiveBuilder archive = new ArchiveBuilder("ensemble-test");
        archive.addClasses(RuntimeType.TOMCAT, AnnotatedContextListener.class);
        archive.addPackage(CommandSupport.class.getPackage());
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                if (ArchiveBuilder.getTargetContainer() == RuntimeType.KARAF) {
                    OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                    builder.addBundleManifestVersion(2);
                    builder.addBundleSymbolicName(archive.getName());
                    builder.addBundleVersion("1.0.0");
                    builder.addManifestHeader(Constants.GRAVIA_ENABLED, Boolean.TRUE.toString());
                    builder.addImportPackages(RuntimeLocator.class, FabricService.class);
                    builder.addImportPackages(AbstractCommand.class, Action.class);
                    builder.addImportPackage("org.apache.felix.service.command;status=provisional");
                    builder.addImportPackages(ConfigurationAdmin.class);
                    return builder.openStream();
                } else {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addIdentityCapability(archive.getName(), "1.0.0");
                    builder.addManifestHeader("Dependencies", "io.fabric8.api,org.apache.karaf,org.jboss.gravia");
                    return builder.openStream();
                }
            }
        });
        return archive.getArchive();
    }

    @Test
    public void testAddAndRemove() throws Exception {
        System.err.println(CommandSupport.executeCommand("fabric:create --force --clean -n"));
        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            Set<Container> containers = ContainerBuilder.create(2).withName("ens").assertProvisioningResult().build();
            try {
                Deque<Container> containerQueue = new LinkedList<Container>(containers);
                Deque<Container> addedContainers = new LinkedList<Container>();

                for (int e = 0; e < 3 && containerQueue.size() >= 2 && containerQueue.size() % 2 == 0; e++) {
                    Container cnt1 = containerQueue.removeFirst();
                    Container cnt2 = containerQueue.removeFirst();
                    addedContainers.add(cnt1);
                    addedContainers.add(cnt2);
                    FabricEnsembleSupport.addToEnsemble(fabricService, cnt1, cnt2);
                    System.err.println(CommandSupport.executeCommand("config:proplist --pid io.fabric8.zookeeper"));

                    System.err.println(CommandSupport.executeCommand("fabric:container-list"));
                    System.err.println(CommandSupport.executeCommand("fabric:ensemble-list"));
                    ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.awaitService(moduleContext, ZooKeeperClusterService.class);
                    Assert.assertNotNull(zooKeeperClusterService);
                    List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                    Assert.assertTrue(ensembleContainersResult.contains(cnt1.getId()));
                    Assert.assertTrue(ensembleContainersResult.contains(cnt2.getId()));
                    Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), FabricEnsembleSupport.PROVISION_TIMEOUT);
                }

                for (int e = 0; e < 3 && addedContainers.size() >= 2 && addedContainers.size() % 2 == 0; e++) {
                    Container cnt1 = addedContainers.removeFirst();
                    Container cnt2 = addedContainers.removeFirst();
                    containerQueue.add(cnt1);
                    containerQueue.add(cnt2);
                    FabricEnsembleSupport.removeFromEnsemble(fabricService, cnt1, cnt2);
                    System.err.println(CommandSupport.executeCommand("config:proplist --pid io.fabric8.zookeeper"));

                    System.err.println(CommandSupport.executeCommand("fabric:container-list"));
                    System.err.println(CommandSupport.executeCommand("fabric:ensemble-list"));
                    ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.awaitService(moduleContext, ZooKeeperClusterService.class);
                    Assert.assertNotNull(zooKeeperClusterService);
                    List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                    Assert.assertFalse(ensembleContainersResult.contains(cnt1.getId()));
                    Assert.assertFalse(ensembleContainersResult.contains(cnt2.getId()));
                    Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), FabricEnsembleSupport.PROVISION_TIMEOUT);
                }
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }
}
