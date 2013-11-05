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

package org.fusesource.fabric.itests.smoke;

import java.util.Map;

import static junit.framework.Assert.assertNotNull;

import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
@Ignore("[FABRIC-647] Fix fabric smoke ProfileEditTest")
public class ProfileEditTest extends FabricTestSupport {

    @Test
    public void testManipulatePid() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        FabricService fabricService = getFabricService();
        assertNotNull(fabricService);
        waitForFabricCommands();
        System.err.println(executeCommand("fabric:profile-edit --pid my.pid/key=value default"));

        //Check that my.pid has been added to the default profile
        Profile profile = fabricService.getDefaultVersion().getProfile("default");
        Assert.assertNotNull(profile);
        Map<String, Map<String, String>> configurations = profile.getConfigurations();
        Assert.assertNotNull(configurations);
        Assert.assertTrue(configurations.containsKey("my.pid"));
        Map<String, String> myPid = configurations.get("my.pid");
        Assert.assertNotNull(myPid);
        Assert.assertTrue(myPid.containsKey("key"));
        Assert.assertEquals("value", myPid.get("key"));


        //Check append options for a pid.
        System.err.println(executeCommand("fabric:profile-edit --append --pid my.pid/key=othervalue default"));
        configurations = profile.getConfigurations();
        Assert.assertTrue(configurations.containsKey("my.pid"));
        myPid = configurations.get("my.pid");
        Assert.assertNotNull(myPid);
        Assert.assertTrue(myPid.containsKey("key"));
        Assert.assertEquals("value,othervalue", myPid.get("key"));

        System.err.println(executeCommand("fabric:profile-edit --remove --pid my.pid/key=value default"));
        configurations = profile.getConfigurations();
        Assert.assertTrue(configurations.containsKey("my.pid"));
        myPid = configurations.get("my.pid");
        Assert.assertNotNull(myPid);
        Assert.assertTrue(myPid.containsKey("key"));
        Assert.assertEquals("othervalue", myPid.get("key"));

        //Check append options for a pid.
        System.err.println(executeCommand("fabric:profile-edit --remove --pid my.pid/key=othervalue default"));
        configurations = profile.getConfigurations();
        Assert.assertTrue(configurations.containsKey("my.pid"));
        myPid = configurations.get("my.pid");
        Assert.assertNotNull(myPid);
        Assert.assertTrue(myPid.containsKey("key"));
        Assert.assertEquals("", myPid.get("key"));

        //Check assign option with '='.
        System.err.println(executeCommand("fabric:profile-edit --pid my.pid/key=prop1=value1 default"));
        configurations = profile.getConfigurations();
        Assert.assertTrue(configurations.containsKey("my.pid"));
        myPid = configurations.get("my.pid");
        Assert.assertNotNull(myPid);
        Assert.assertTrue(myPid.containsKey("key"));
        Assert.assertEquals("prop1=value1", myPid.get("key"));

        //Check multiple properties
        System.err.println(executeCommand("fabric:profile-edit --pid my.pid/key1=value1 --pid my.pid/key2=value2 default"));
        configurations = profile.getConfigurations();
        Assert.assertTrue(configurations.containsKey("my.pid"));
        myPid = configurations.get("my.pid");
        Assert.assertNotNull(myPid);
        Assert.assertTrue(myPid.containsKey("key1"));
        Assert.assertEquals("value1", myPid.get("key1"));
        Assert.assertTrue(myPid.containsKey("key2"));
        Assert.assertEquals("value2", myPid.get("key2"));
    }


    @Test
    public void testImportPid() throws Exception {
        System.err.println(executeCommands("config:edit my.pid","config:propset key1 value1","config:propset key2 value2", "config:update"));
        System.err.println(executeCommand("fabric:create -n"));
        FabricService fabricService = getFabricService();
        assertNotNull(fabricService);
        waitForFabricCommands();
        System.err.println(executeCommand("fabric:profile-edit --pid my.pid --import-pid default"));

        //Check that my.pid has been added to the default profile
        Profile profile = fabricService.getDefaultVersion().getProfile("default");
        Assert.assertNotNull(profile);
        Map<String, Map<String, String>> configurations = profile.getConfigurations();
        Assert.assertNotNull(configurations);
        Assert.assertTrue(configurations.containsKey("my.pid"));
        Map<String, String> myPid = configurations.get("my.pid");
        Assert.assertNotNull(myPid);
        Assert.assertTrue(myPid.containsKey("key1"));
        Assert.assertEquals("value1", myPid.get("key1"));
        Assert.assertTrue(myPid.containsKey("key2"));
        Assert.assertEquals("value2", myPid.get("key2"));

        System.err.println(executeCommand("fabric:profile-edit --pid my.pid/key1 --delete default"));
        Map<String,String> configuration = profile.getConfiguration("my.pid");
        Assert.assertFalse(configuration.containsKey("key1"));

        System.err.println(executeCommand("fabric:profile-edit --pid my.pid --delete default"));
        configurations = profile.getConfigurations();
        Assert.assertFalse(configurations.containsKey("my.pid"));
    }


    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration())
        };    }
}
