/*
 * Copyright (C) 2011 FuseSource, Corp. All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the CDDL license
 * a copy of which has been included with this distribution in the license.txt file.
 */

package org.fusesource.fabric.itests.paxexam;

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


import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;

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
        Thread.sleep(DEFAULT_WAIT);
        System.err.println(executeCommand("shell:source mvn:org.fusesource.fabric/fuse-fabric/1.1-SNAPSHOT/karaf/profiles"));
        System.err.println(executeCommand("fabric:agent-create --parent root --profile camel child1"));
        Thread.sleep(3 * DEFAULT_WAIT);
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
                logLevel(LogLevelOption.LogLevel.ERROR)};
    }
}
