/*
 * Copyright (C) 2011 FuseSource, Corp. All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the CDDL license
 * a copy of which has been included with this distribution in the license.txt file.
 */
package org.fusesource.fabric.itests.paxexam;

import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.FabricService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.linkedin.zookeeper.client.IZKClient;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ContainerStartupTest extends FabricCommandsTestSupport {

    @Test
    public void testLocalFabricCluster() throws Exception {
        FabricService fabricService = getOsgiService(FabricService.class);
        assertNotNull(fabricService);

        executeCommand("fabric:ensemble-create --clean root");

        //Wait for zookeeper service to become available.
        IZKClient zooKeeper = getOsgiService(IZKClient.class);

        Thread.sleep(DEFAULT_WAIT);

        Agent[] agents = fabricService.getAgents();

        assertNotNull(agents);
        assertEquals("Expected to find 1 agent",1,agents.length);
        assertEquals("Expected to find the root agent","root",agents[0].getId());


    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                fabricDistributionConfiguration(), keepRuntimeFolder()
                ,logLevel(LogLevelOption.LogLevel.ERROR)};
    }
}
