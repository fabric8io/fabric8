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

package io.fabric8.itests.basic.cloud;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;
import io.fabric8.api.ServiceLocator;
import io.fabric8.itests.paxexam.support.FabricTestSupport;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import com.google.common.base.Predicate;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricRackspaceContainerTest extends FabricTestSupport {

    private String identity;
    private String credential;
    private String image;
    private String location;
    private String user;
    private String group = "fabricitests";

    /**
     * Returns true if all the requirements for running this test are meet.
     * @return
     */
    public boolean isReady() {
        return
                identity != null && credential != null && image != null & user != null &&
                        !identity.isEmpty() && !credential.isEmpty() && !image.isEmpty() && !user.isEmpty();
    }

    @Before
    public void setUp() {
        identity = System.getProperty("fabricitest.rackspace.identity");
        credential = System.getProperty("fabricitest.rackspace.credential");
        image = System.getProperty("fabricitest.rackspace.image");
        location = System.getProperty("fabricitest.rackspace.location");
        user = System.getProperty("fabricitest.rackspace.user");
    }


    @After
    public void tearDown() {
        if (isReady()) {
            System.err.println(executeCommand("group-destroy " + group, 30000L, false));
        }
    }

    /**
     * Starts an ensemble server on EC2, configures the security groups and join the ensemble.
     *
     * @throws InterruptedException
     * @throws java.io.IOException
     */
    @Test
    public void testRackspaceAgentCreation() throws InterruptedException, IOException {
        if (!isReady()) {
            System.err.println("Rackspace is not setup correctly. This test will not run.");
            System.err.println("To properly run this test, you need to setup with maven the following properties:");
            System.err.println("fabricitest.rackspace.identity \t The rackspace access id");
            System.err.println("fabricitest.rackspace.credential \t The rackspace access key");
            System.err.println("fabricitest.rackspace.image  \t The rackspace (java ready) image");
            System.err.println("fabricitest.rackspace.user  \t The user under which the agent will run");
            return;
        }

        System.err.println(executeCommand("features:install jclouds-cloudserver-us fabric-jclouds jclouds-commands"));

        executeCommand("fabric:cloud-service-add --provider cloudservers-us --identity "+identity+" --credential "+credential);

        ComputeService computeService = ServiceLocator.awaitService(bundleContext, ComputeService.class, 3, TimeUnit.MINUTES);

        //The compute service needs some time to properly initialize.
        System.err.println(executeCommand(String.format("fabric:container-create-cloud --provider cloudservers-us --group %s --ensemble-server ensemble1", group), 10 * 60000L, false));
        String publicIp = getNodePublicIp(computeService);
        assertNotNull(publicIp);
        System.err.println(executeCommand("fabric:join -n " + publicIp + ":2181", 10 * 60000L, false));
        Thread.sleep(DEFAULT_TIMEOUT);
        System.err.println(executeCommand("fabric:join " + publicIp + ":2181", 10 * 60000L, false));
        String agentList = executeCommand("fabric:container-list");
        System.err.println(agentList);
        assertTrue(agentList.contains("root") && agentList.contains("ensemble1"));

    }

    /**
     * Return the public ip of the generated node.
     * It assumes that no other node is currently running using the current group.
     * @return
     */
    private String getNodePublicIp(ComputeService computeService) {

        for (ComputeMetadata computeMetadata : computeService.listNodesDetailsMatching(new Predicate<ComputeMetadata>() {
            @Override
            public boolean apply(ComputeMetadata metadata) {
                NodeMetadata nodeMetadata = (NodeMetadata) metadata;
                return nodeMetadata.getGroup().equals(group) && nodeMetadata.getStatus().equals(NodeMetadata.Status.RUNNING);
            }
        })) {
            NodeMetadata nodeMetadata = (NodeMetadata) computeMetadata;
            return nodeMetadata.getPublicAddresses().toArray(new String[0])[0];

        }
        return null;
    }

    /**
     * @return the IP address of the client on which this code is running.
     * @throws java.io.IOException
     */
    protected String getOriginatingIp() throws IOException {
        URL url = new URL("http://checkip.amazonaws.com/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        return IOUtils.toString(connection.getInputStream()).trim() + "/32";
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                copySystemProperty("fabricitest.rackspace.identity"),
                copySystemProperty("fabricitest.rackspace.credential"),
                copySystemProperty("fabricitest.rackspace.image"),
                copySystemProperty("fabricitest.rackspace.location"),
                copySystemProperty("fabricitest.rackspace.user"),
                editConfigurationFileExtend("etc/config.properties", "org.osgi.framework.executionenvironment", "JavaSE-1.7,JavaSE-1.6,JavaSE-1.5"),
                scanFeatures("jclouds","jclouds-compute").start()
        };
    }
}
