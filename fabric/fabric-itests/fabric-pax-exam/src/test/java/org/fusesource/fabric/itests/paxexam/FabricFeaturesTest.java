/*
 * Copyright (C) 2011 FuseSource, Corp. All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the CDDL license
 * a copy of which has been included with this distribution in the license.txt file.
 */

package org.fusesource.fabric.itests.paxexam;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.ZooKeeperClusterService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.linkedin.zookeeper.client.IZKClient;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.debugConfiguration;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricFeaturesTest extends FabricCommandsTestSupport {

    @After
    public void tearDown() throws InterruptedException {
       destroyChildAgent("child1");
    }

    @Test
    public void testFeatureProvisioning() throws Exception {
         //Wait for zookeeper service to become available.
        IZKClient zooKeeper = getOsgiService(IZKClient.class);

        FabricService fabricService = getOsgiService(FabricService.class);
        assertNotNull(fabricService);
        createChildAgent("child1");
        Thread.sleep(DEFAULT_WAIT);
        System.err.println(executeCommand("fabric:profile-edit -p default --repositories mvn:org.apache.karaf.assemblies.features/standard/2.2.2-fuse-02-06/xml/features"));
        System.err.println(executeCommand("fabric:profile-edit -p default --repositories mvn:org.apache.karaf.assemblies.features/enterprise/2.2.2-fuse-02-06/xml/features"));
        System.err.println(executeCommand("fabric:profile-edit -p default --repositories mvn:org.fusesource.fabric/fuse-fabric/1.1-SNAPSHOT/xml/features"));

        System.err.println(executeCommand("fabric:profile-edit -p default --repositories mvn:org.apache.camel.karaf/apache-camel/2.8.0-fuse-01-06/xml/features"));
        System.err.println(executeCommand("fabric:profile-edit -p default --features camel-core/2.8.0-fuse-01-06"));
        System.err.println(executeCommand("fabric:profile-edit -p default --features camel-blueprint/2.8.0-fuse-01-06"));
        System.err.println(executeCommand("fabric:profile-display default"));
        Thread.sleep(DEFAULT_WAIT);
        Thread.sleep(DEFAULT_WAIT);

        System.err.println(executeCommand("fabric:agent-connect -u admin -p admin child1 osgi:list -t 0"));
        String camelBundleCount = executeCommand("fabric:agent-connect -u admin -p admin child1 osgi:list -t 0| grep -c -i camel");
        int count = Integer.parseInt(camelBundleCount.trim());
        assertTrue("At least one camel bundle is expected", count >= 1);
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                fabricDistributionConfiguration(), keepRuntimeFolder(),
                new VMOption("-D"+ZooKeeperClusterService.CLUSTER_AUTOSTART_PROPERTY+"=true") ,
                //debugConfiguration("5005",true) ,
                logLevel(LogLevelOption.LogLevel.ERROR)};
    }
}
