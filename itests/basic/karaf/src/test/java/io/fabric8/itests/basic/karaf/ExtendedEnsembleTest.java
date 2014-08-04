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
package io.fabric8.itests.basic.karaf;

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
import java.util.Random;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(Arquillian.class)
@Ignore("[FABRIC-1128] Git TransportException in basic karaf tests")
public class ExtendedEnsembleTest {

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final ArchiveBuilder archive = new ArchiveBuilder("extended-ensemble-test");
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
    @SuppressWarnings("unchecked")
    public void testAddAndRemoveWithVersions() throws Exception {
        System.err.println(CommandSupport.executeCommand("fabric:create --force --clean -n"));
        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            System.out.println(CommandSupport.executeCommand("fabric:version-create --parent 1.0 1.1"));
            System.out.println(CommandSupport.executeCommand("fabric:container-upgrade --all 1.1"));
            
            Set<Container> containers = ContainerBuilder.create(2).withName("basic.ensA").withProfiles("default").assertProvisioningResult().build(fabricService);
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
                    ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.awaitService(ZooKeeperClusterService.class);
                    Assert.assertNotNull(zooKeeperClusterService);
                    List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                    Assert.assertTrue(ensembleContainersResult.contains(cnt1.getId()));
                    Assert.assertTrue(ensembleContainersResult.contains(cnt2.getId()));
                    Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), FabricEnsembleSupport.PROVISION_TIMEOUT);
                }

                System.out.println(CommandSupport.executeCommand("fabric:version-create"));
                System.out.println(CommandSupport.executeCommand("fabric:container-upgrade --all 1.2"));

                for (int e = 0; e < 3 && addedContainers.size() >= 2 && addedContainers.size() % 2 == 0; e++) {
                    Container cnt1 = addedContainers.removeFirst();
                    Container cnt2 = addedContainers.removeFirst();
                    containerQueue.add(cnt1);
                    containerQueue.add(cnt2);
                    FabricEnsembleSupport.removeFromEnsemble(fabricService, cnt1, cnt2);
                    System.err.println(CommandSupport.executeCommand("config:proplist --pid io.fabric8.zookeeper"));

                    System.err.println(CommandSupport.executeCommand("fabric:container-list"));
                    System.err.println(CommandSupport.executeCommand("fabric:ensemble-list"));
                    ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.awaitService(ZooKeeperClusterService.class);
                    Assert.assertNotNull(zooKeeperClusterService);
                    List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                    Assert.assertFalse(ensembleContainersResult.contains(cnt1.getId()));
                    Assert.assertFalse(ensembleContainersResult.contains(cnt2.getId()));
                    Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), FabricEnsembleSupport.PROVISION_TIMEOUT);
                }
            } finally {
                ContainerBuilder.stop(fabricService, containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    /**
     * We want to test the ensemble health is not affected if part of the ensemble is switched to an other version.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testAddAndRemoveWithPartialVersionUpgrades() throws Exception {
        System.out.println(CommandSupport.executeCommand("fabric:create --force --clean -n"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            
            System.out.println(CommandSupport.executeCommand("fabric:version-create"));
            System.out.println(CommandSupport.executeCommand("fabric:container-upgrade --all 1.1"));

            Set<Container> containers = ContainerBuilder.create(2).withName("basic.ensB").withProfiles("fabric").assertProvisioningResult().build(fabricService);
            try {
                LinkedList<Container> containerQueue = new LinkedList<Container>(containers);
                LinkedList<Container> addedContainers = new LinkedList<Container>();

                System.out.println(CommandSupport.executeCommand("fabric:version-create"));
                System.out.println(CommandSupport.executeCommand("fabric:container-upgrade --all 1.2"));


                Random rand = new Random();

                for (int version = 3; version < 5; version++) {

                    Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), FabricEnsembleSupport.PROVISION_TIMEOUT);

                    for (int e = 0; e < 3 && containerQueue.size() >= 2 && containerQueue.size() % 2 == 0; e++) {
                        Container cnt1 = containerQueue.removeFirst();
                        Container cnt2 = containerQueue.removeFirst();
                        addedContainers.add(cnt1);
                        addedContainers.add(cnt2);
                        FabricEnsembleSupport.addToEnsemble(fabricService, cnt1, cnt2);
                        System.out.println(CommandSupport.executeCommand("config:proplist --pid io.fabric8.zookeeper"));

                        System.out.println(CommandSupport.executeCommand("fabric:container-list"));
                        System.out.println(CommandSupport.executeCommand("fabric:ensemble-list"));
                        ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.awaitService(ZooKeeperClusterService.class);
                        org.junit.Assert.assertNotNull(zooKeeperClusterService);
                        List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                        org.junit.Assert.assertTrue(ensembleContainersResult.contains(cnt1.getId()));
                        org.junit.Assert.assertTrue(ensembleContainersResult.contains(cnt2.getId()));
                        Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), FabricEnsembleSupport.PROVISION_TIMEOUT);
                    }


                    int index = rand.nextInt(addedContainers.size());
                    String randomContainer = addedContainers.get(index).getId();
                    System.out.println(CommandSupport.executeCommand("fabric:version-create 1." + version));
                    System.out.println(CommandSupport.executeCommand("fabric:container-upgrade 1." + version + " " + randomContainer));

                    Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), FabricEnsembleSupport.PROVISION_TIMEOUT);

                    for (int e = 0; e < 3 && addedContainers.size() >= 2 && addedContainers.size() % 2 == 0; e++) {
                        Container cnt1 = addedContainers.removeFirst();
                        Container cnt2 = addedContainers.removeFirst();
                        containerQueue.add(cnt1);
                        containerQueue.add(cnt2);
                        FabricEnsembleSupport.removeFromEnsemble(fabricService, cnt1, cnt2);
                        System.out.println(CommandSupport.executeCommand("config:proplist --pid io.fabric8.zookeeper"));

                        System.out.println(CommandSupport.executeCommand("fabric:container-list"));
                        System.out.println(CommandSupport.executeCommand("fabric:ensemble-list"));
                        ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.awaitService(ZooKeeperClusterService.class);
                        org.junit.Assert.assertNotNull(zooKeeperClusterService);
                        List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                        org.junit.Assert.assertFalse(ensembleContainersResult.contains(cnt1.getId()));
                        org.junit.Assert.assertFalse(ensembleContainersResult.contains(cnt2.getId()));
                        Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), FabricEnsembleSupport.PROVISION_TIMEOUT);
                    }
                    System.out.println(CommandSupport.executeCommand("fabric:container-rollback 1." + (version - 1) + " " + randomContainer));
                }
            } finally {
                ContainerBuilder.stop(fabricService, containers);
            }
        } finally {
            fabricProxy.close();
        }
    }
}
