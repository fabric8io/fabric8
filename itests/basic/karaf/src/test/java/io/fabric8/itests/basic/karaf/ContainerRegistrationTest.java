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
import io.fabric8.api.gravia.ServiceLocator;
import io.fabric8.itests.support.CommandSupport;
import io.fabric8.itests.support.ContainerBuilder;
import io.fabric8.itests.support.ProvisionSupport;
import io.fabric8.itests.support.ServiceProxy;

import java.io.InputStream;
import java.util.Set;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;

/**
 * A test for making sure that the container registration info such as jmx url and ssh url are updated, if new values
 * are assigned to them via the profile.
 */
@RunWith(Arquillian.class)
public class ContainerRegistrationTest {

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "container-registration-test");
        archive.addPackage(CommandSupport.class.getPackage());
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleVersion("1.0.0");
                builder.addImportPackages(ServiceLocator.class, FabricService.class);
                builder.addImportPackages(AbstractCommand.class, Action.class);
                builder.addImportPackage("org.apache.felix.service.command;status=provisional");
                builder.addImportPackages(ConfigurationAdmin.class, ServiceTracker.class, Logger.class);
                return builder.openStream();
            }
        });
        return archive;
    }


    @Test
    @SuppressWarnings("unchecked")
    public void testContainerRegistration() throws Exception {
        System.out.println(CommandSupport.executeCommand("fabric:create --force --clean -n"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        BundleContext moduleContext = ServiceLocator.getSystemContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            
            System.out.println(CommandSupport.executeCommand("fabric:profile-create --parents default child-profile"));
            Assert.assertTrue(ProvisionSupport.profileAvailable("child-profile", "1.0", ProvisionSupport.PROVISION_TIMEOUT));

            Set<Container> containers = ContainerBuilder.create(1,1).withName("basic.cntA").withProfiles("child-profile").assertProvisioningResult().build(fabricService);
            try {
                Container cntA = containers.iterator().next();
                System.out.println(CommandSupport.executeCommand("fabric:profile-edit --import-pid --pid org.apache.karaf.shell child-profile"));
                System.out.println(CommandSupport.executeCommand("fabric:profile-edit --pid org.apache.karaf.shell/sshPort=8105 child-profile"));

                System.out.println(CommandSupport.executeCommand("fabric:profile-edit --import-pid --pid org.apache.karaf.management child-profile"));
                System.out.println(CommandSupport.executeCommand("fabric:profile-edit --pid org.apache.karaf.management/rmiServerPort=55555 child-profile"));
                System.out.println(CommandSupport.executeCommand("fabric:profile-edit --pid org.apache.karaf.management/rmiRegistryPort=1100 child-profile"));
                System.out.println(CommandSupport.executeCommand("fabric:profile-edit --pid org.apache.karaf.management/serviceUrl=service:jmx:rmi://localhost:55555/jndi/rmi://localhost:1099/karaf-"+cntA.getId()+" child-profile"));

                String sshUrl = cntA.getSshUrl();
                String jmxUrl = cntA.getJmxUrl();
                
                long end = System.currentTimeMillis() + ProvisionSupport.PROVISION_TIMEOUT;
                while (System.currentTimeMillis() < end && (!sshUrl.endsWith("8105") || !jmxUrl.contains("55555"))) {
                    Thread.sleep(1000L);
                    sshUrl = cntA.getSshUrl();
                    jmxUrl = cntA.getJmxUrl();
                }
                
                Assert.assertTrue("sshUrl ends with 8105, but was: " + sshUrl, sshUrl.endsWith("8105"));
                Assert.assertTrue("jmxUrl contains 55555, but was: " + jmxUrl, jmxUrl.contains("55555"));
            } finally {
                ContainerBuilder.stop(fabricService, containers);
            }
        } finally {
            fabricProxy.close();
        }
    }
}
