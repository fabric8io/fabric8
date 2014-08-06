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
package io.fabric8.itests.smoke.karaf;

import static org.junit.Assert.assertTrue;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
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
import org.jboss.gravia.Constants;
import org.jboss.gravia.itests.support.AnnotatedContextListener;
import org.jboss.gravia.itests.support.ArchiveBuilder;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;

/**
 * The purpose of this test is to make sure that everything can be downloaded from the fabric-maven-proxy.
 * Also we want to make sure that after artifacts have been downloaded can be properlly used, for example:
 * Feature Repositories can be properly resolved.
 *
 * Note: This test makes sense to run using remote containers that have an empty maven repo.
 *
 * http://fusesource.com/issues/browse/FABRIC-398
 */
@RunWith(Arquillian.class)
public class DeploymentAgentTest {

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final ArchiveBuilder archive = new ArchiveBuilder("deployment-agent-test");
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
                    builder.addImportPackages(ConfigurationAdmin.class, Logger.class);
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
    public void testFeatureRepoResolution() throws Exception {
        CommandSupport.executeCommand("fabric:create --force --clean -n");

        //We are just want to use a feature repository that is not part of the distribution.
        CommandSupport.executeCommand("fabric:profile-create --parents feature-camel test-profile");
        CommandSupport.executeCommand("fabric:version-create --parent 1.0 1.1");
        CommandSupport.executeCommand("fabric:profile-edit --repositories mvn:io.fabric8.examples.fabric-camel-dosgi/features/" + System.getProperty("fabric.version") + "/xml/features test-profile 1.1");
        CommandSupport.executeCommand("fabric:profile-edit --features fabric-dosgi test-profile 1.1");
        //We remove all repositories from agent config but the maven central to rely on the fabric-maven-proxy.
        //Also remove local repository
        CommandSupport.executeCommand("fabric:profile-edit --pid io.fabric8.agent/org.ops4j.pax.url.mvn.repositories=http://repo1.maven.org/maven2@id=m2central default 1.1");
        CommandSupport.executeCommand("fabric:profile-edit --pid test-profile 1.1");

        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            Set<Container> containers = ContainerBuilder.create().withName("smoke.cntA").withProfiles("test-profile").assertProvisioningResult().build(fabricService);
            try {
                //We want to remove all repositories from fabric-agent.
                for (Container container : containers) {
                    CommandSupport.executeCommand("fabric:container-upgrade 1.1 " + container.getId());
                    System.out.flush();
                }
                ProvisionSupport.provisioningSuccess(containers, ProvisionSupport.PROVISION_TIMEOUT);
                CommandSupport.executeCommand("fabric:container-list");

                for (Container container : containers) {
                    CommandSupport.executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " osgi:list");
                    CommandSupport.executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " config:proplist --pid org.ops4j.pax.url.mvn");
                    System.out.flush();
                }
            } finally {
                ContainerBuilder.stop(fabricService, containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResolveOptionalImports() throws Exception {
        CommandSupport.executeCommand("fabric:create --force --clean -n");

        CommandSupport.executeCommand("fabric:profile-create --parents default test-profile");
        CommandSupport.executeCommand("fabric:profile-edit --pid io.fabric8.agent/resolve.optional.imports=true test-profile");
        CommandSupport.executeCommand("fabric:profile-edit --features spring-struts test-profile");

        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            Set<Container> containers = ContainerBuilder.create().withName("smoke.cntB").withProfiles("test-profile").assertProvisioningResult().build(fabricService);
            try {
                String command = "fabric:container-connect -u admin -p admin " + containers.iterator().next().getId() + " osgi:list -s | grep org.apache.servicemix.bundles.struts";
                String result = CommandSupport.executeCommand(command);
                assertTrue("Result contains struts, but was: " + result, result.contains("org.apache.servicemix.bundles.struts"));
            } finally {
                ContainerBuilder.stop(fabricService, containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

}
