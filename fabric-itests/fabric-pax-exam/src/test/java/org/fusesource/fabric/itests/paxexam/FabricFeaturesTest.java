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

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricFeaturesTest extends FabricCommandsTestSupport {


    @Test
    public void testAgentCreation() throws Exception {
         //Wait for zookeeper service to become available.
        IZKClient zooKeeper = getOsgiService(IZKClient.class);

        FabricService fabricService = getOsgiService(FabricService.class);
        assertNotNull(fabricService);

        executeCommand("fabric:edit-profile -p default --features war");
        Thread.sleep(DEFAULT_WAIT);

        FeaturesService featuresService = getOsgiService(FeaturesService.class);
        Feature warFeature = featuresService.getFeature("war");
        Boolean isWarInstalled = featuresService.isInstalled(warFeature);
        //assertTrue(isWarInstalled);
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                fabricDistributionConfiguration(), keepRuntimeFolder(),
                new VMOption("-D"+ZooKeeperClusterService.CLUSTER_AUTOSTART_PROPERTY+"=true") ,
                //debugConfiguration("5005",true) ,
                logLevel(LogLevelOption.LogLevel.INFO)};
    }
}
