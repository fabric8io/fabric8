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

import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.gravia.ServiceLocator;
import io.fabric8.itests.support.CommandSupport;
import io.fabric8.itests.support.ServiceProxy;

import java.io.InputStream;
import java.util.Map;

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

@RunWith(Arquillian.class)
public class ProfileEditTest {

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "profile-edit-test");
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
    public void testManipulatePid() throws Exception {
        System.err.println(CommandSupport.executeCommand("fabric:create --force --clean -n"));
        BundleContext moduleContext = ServiceLocator.getSystemContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();

            System.err.println(CommandSupport.executeCommand("fabric:profile-edit --pid my.pid/key=value default"));

            //Check that my.pid has been added to the default profile
            Profile profile = fabricService.getRequiredDefaultVersion().getRequiredProfile("default");
            Assert.assertNotNull(profile);
            Map<String, Map<String, String>> configurations = profile.getConfigurations();
            Assert.assertNotNull(configurations);
            Assert.assertTrue(configurations.containsKey("my.pid"));
            Map<String, String> myPid = configurations.get("my.pid");
            Assert.assertNotNull(myPid);
            Assert.assertTrue(myPid.containsKey("key"));
            Assert.assertEquals("value", myPid.get("key"));

            //Check append options for a pid.
            System.err.println(CommandSupport.executeCommand("fabric:profile-edit --append --pid my.pid/key=othervalue default"));
            profile = fabricService.getRequiredDefaultVersion().getRequiredProfile("default");
            configurations = profile.getConfigurations();
            Assert.assertTrue(configurations.containsKey("my.pid"));
            myPid = configurations.get("my.pid");
            Assert.assertNotNull(myPid);
            Assert.assertTrue(myPid.containsKey("key"));
            Assert.assertEquals("value,othervalue", myPid.get("key"));

            System.err.println(CommandSupport.executeCommand("fabric:profile-edit --remove --pid my.pid/key=value default"));
            profile = fabricService.getRequiredDefaultVersion().getRequiredProfile("default");
            configurations = profile.getConfigurations();
            Assert.assertTrue(configurations.containsKey("my.pid"));
            myPid = configurations.get("my.pid");
            Assert.assertNotNull(myPid);
            Assert.assertTrue(myPid.containsKey("key"));
            Assert.assertEquals("othervalue", myPid.get("key"));

            //Check append options for a pid.
            System.err.println(CommandSupport.executeCommand("fabric:profile-edit --remove --pid my.pid/key=othervalue default"));
            profile = fabricService.getRequiredDefaultVersion().getRequiredProfile("default");
            configurations = profile.getConfigurations();
            Assert.assertTrue(configurations.containsKey("my.pid"));
            myPid = configurations.get("my.pid");
            Assert.assertNotNull(myPid);
            Assert.assertTrue(myPid.containsKey("key"));
            Assert.assertEquals("", myPid.get("key"));

            //Check assign option with '='.
            System.err.println(CommandSupport.executeCommand("fabric:profile-edit --pid my.pid/key=prop1=value1 default"));
            profile = fabricService.getRequiredDefaultVersion().getRequiredProfile("default");
            configurations = profile.getConfigurations();
            Assert.assertTrue(configurations.containsKey("my.pid"));
            myPid = configurations.get("my.pid");
            Assert.assertNotNull(myPid);
            Assert.assertTrue(myPid.containsKey("key"));
            Assert.assertEquals("prop1=value1", myPid.get("key"));

            //Check multiple properties
            System.err.println(CommandSupport.executeCommand("fabric:profile-edit --pid my.pid/key1=value1 --pid my.pid/key2=value2 default"));
            profile = fabricService.getRequiredDefaultVersion().getRequiredProfile("default");
            configurations = profile.getConfigurations();
            Assert.assertTrue(configurations.containsKey("my.pid"));
            myPid = configurations.get("my.pid");
            Assert.assertNotNull(myPid);
            Assert.assertTrue(myPid.containsKey("key1"));
            Assert.assertEquals("value1", myPid.get("key1"));
            Assert.assertTrue(myPid.containsKey("key2"));
            Assert.assertEquals("value2", myPid.get("key2"));

            //Check import pid
            System.err.println(CommandSupport.executeCommands("config:edit my.pid2", "config:propset key1 value1", "config:propset key2 value2", "config:update"));
            System.err.println(CommandSupport.executeCommand("fabric:profile-edit --pid my.pid2 --import-pid default"));

            //Check that my.pid has been added to the default profile
            profile = fabricService.getRequiredDefaultVersion().getRequiredProfile("default");
            Assert.assertNotNull(profile);
            configurations = profile.getConfigurations();
            Assert.assertNotNull(configurations);
            Assert.assertTrue(configurations.containsKey("my.pid2"));
            Map<String, String> myPid2 = configurations.get("my.pid2");
            System.out.println("my.pid2 => " + myPid2);
            Assert.assertNotNull(myPid2);
            Assert.assertTrue(myPid2.containsKey("key1"));
            Assert.assertEquals("value1", myPid2.get("key1"));
            Assert.assertTrue(myPid2.containsKey("key2"));
            Assert.assertEquals("value2", myPid2.get("key2"));

            System.err.println(CommandSupport.executeCommand("fabric:profile-edit --pid my.pid2/key1 --delete default"));
            profile = fabricService.getRequiredDefaultVersion().getRequiredProfile("default");
            Map<String, String> configuration = profile.getConfiguration("my.pid2");
            Assert.assertFalse(configuration.containsKey("key1"));

            System.err.println(CommandSupport.executeCommand("fabric:profile-edit --pid my.pid2 --delete default"));
            profile = fabricService.getRequiredDefaultVersion().getRequiredProfile("default");
            configurations = profile.getConfigurations();
            Assert.assertFalse(configurations.containsKey("my.pid2"));
        } finally {
            fabricProxy.close();
        }
    }
}
