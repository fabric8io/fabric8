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
import io.fabric8.itests.support.CommandSupport;
import io.fabric8.itests.support.EnsembleSupport;
import io.fabric8.itests.support.ProvisionSupport;
import io.fabric8.itests.support.ServiceProxy;

import java.io.InputStream;
import java.util.Arrays;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.karaf.admin.AdminService;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;

@RunWith(Arquillian.class)
public class ExtendedJoinTest {

    private static final String WAIT_FOR_JOIN_SERVICE = "wait-for-service io.fabric8.boot.commands.service.JoinAvailable";

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final ArchiveBuilder archive = new ArchiveBuilder("extended-join-test");
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
                    builder.addImportPackages(ConfigurationAdmin.class, AdminService.class, Logger.class);
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

	/**
	 * This is a test for FABRIC-353.
	 */
	@Test
	public void testJoinAndAddToEnsemble() throws Exception {
        System.err.println(CommandSupport.executeCommand("fabric:create --force --clean -n"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            AdminService adminService = ServiceLocator.awaitService(AdminService.class);
            String version = System.getProperty("fabric.version");
            System.out.println(CommandSupport.executeCommand("admin:create --featureURL mvn:io.fabric8/fabric8-karaf/" + version + "/xml/features --feature fabric-git --feature fabric-agent --feature fabric-boot-commands basic.cntD"));
            System.out.println(CommandSupport.executeCommand("admin:create --featureURL mvn:io.fabric8/fabric8-karaf/" + version + "/xml/features --feature fabric-git --feature fabric-agent --feature fabric-boot-commands basic.cntF"));
            try {
                System.out.println(CommandSupport.executeCommand("admin:start basic.cntD"));
                System.out.println(CommandSupport.executeCommand("admin:start basic.cntF"));
                ProvisionSupport.instanceStarted(Arrays.asList("basic.cntD", "basic.cntF"), ProvisionSupport.PROVISION_TIMEOUT);
                System.out.println(CommandSupport.executeCommand("admin:list"));
                String joinCommand = "fabric:join -f --zookeeper-password "+ fabricService.getZookeeperPassword() +" " + fabricService.getZookeeperUrl();

                String response = "";
                for (int i = 0; i < 10 && !response.contains("true"); i++) {
                    response = CommandSupport.executeCommand("ssh:ssh -l karaf -P karaf -p " + adminService.getInstance("basic.cntD").getSshPort() + " localhost " + WAIT_FOR_JOIN_SERVICE);
                    Thread.sleep(1000);
                }
                response = "";
                for (int i = 0; i < 10 && !response.contains("true"); i++) {
                    response = CommandSupport.executeCommand("ssh:ssh -l karaf -P karaf -p " + adminService.getInstance("basic.cntF").getSshPort() + " localhost " + WAIT_FOR_JOIN_SERVICE);
                    Thread.sleep(1000);
                }

                System.err.println(CommandSupport.executeCommand("ssh:ssh -l karaf -P karaf -p " + adminService.getInstance("basic.cntD").getSshPort() + " localhost " + joinCommand));
                System.err.println(CommandSupport.executeCommand("ssh:ssh -l karaf -P karaf -p " + adminService.getInstance("basic.cntF").getSshPort() + " localhost " + joinCommand));
                ProvisionSupport.containersExist(Arrays.asList("basic.cntD", "basic.cntF"), ProvisionSupport.PROVISION_TIMEOUT);
                Container cntD = fabricService.getContainer("basic.cntD");
                Container cntF = fabricService.getContainer("basic.cntF");
                ProvisionSupport.containerStatus(Arrays.asList(cntD, cntF), "success", ProvisionSupport.PROVISION_TIMEOUT);
                EnsembleSupport.addToEnsemble(fabricService, cntD, cntF);
                System.out.println(CommandSupport.executeCommand("fabric:container-list"));
                EnsembleSupport.removeFromEnsemble(fabricService, cntD, cntF);
                System.out.println(CommandSupport.executeCommand("fabric:container-list"));
            } finally {
                System.out.println(CommandSupport.executeCommand("admin:stop basic.cntD"));
                System.out.println(CommandSupport.executeCommand("admin:stop basic.cntF"));
            }
        } finally {
            fabricProxy.close();
        }
	}
}
