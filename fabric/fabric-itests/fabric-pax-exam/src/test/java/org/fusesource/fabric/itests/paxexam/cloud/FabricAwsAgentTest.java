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

package org.fusesource.fabric.itests.paxexam.cloud;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import org.apache.commons.io.IOUtils;
import org.fusesource.fabric.itests.paxexam.FabricTestSupport;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.domain.IpProtocol;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricAwsAgentTest extends FabricTestSupport {

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
        identity = System.getProperty("fabricitest.aws.identity");
        credential = System.getProperty("fabricitest.aws.credential");
        image = System.getProperty("fabricitest.aws.image");
        location = System.getProperty("fabricitest.aws.location");
        user = System.getProperty("fabricitest.aws.user");
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
     * @throws IOException
     */
    @Test
    public void testAwsAgentCreation() throws InterruptedException, IOException {
        if (!isReady()) {
            System.err.println("Amazon is not setup correctly. This test will not run.");
            System.err.println("To properly run this test, you need to setup with maven the following properties:");
            System.err.println("fabricitest.aws.identity \t The aws access id");
            System.err.println("fabricitest.aws.credential \t The aws access key");
            System.err.println("fabricitest.aws.image  \t The aws (java ready) image");
            System.err.println("fabricitest.aws.user  \t The user under which the agent will run");
            return;
        }

        System.err.println(executeCommand("features:install jclouds-aws-ec2 fabric-jclouds jclouds-commands"));

        String fabricVersion = System.getProperty("fabric.version");
        if (fabricVersion != null && fabricVersion.contains("SNAPSHOT")) {
            System.err.println("Switching to snapshot repository");
            executeCommands("config:propset --pid org.fusesource.fabric.service defaultRepo http://repo.fusesource.com/nexus/content/groups/public-snapshots/");
        }

        //Filtering out regions because there is a temporary connectivity issue with us-west-2.
        executeCommands("config:edit org.jclouds.compute-ec2",
                "config:propset provider aws-ec2 ",
                "config:propset identity " + identity,
                "config:propset credential " + credential,
                "config:propset jclouds.regions eu-west-1,us-west-1,us-east-1",
                "config:update");

        ComputeService computeService = getOsgiService(ComputeService.class, 3 * DEFAULT_TIMEOUT);

        setUpSecurityGroup(computeService, 2181);

        //The compute service needs some time to properly initialize.
        Thread.sleep(3 * DEFAULT_TIMEOUT);
        System.err.println(executeCommand(String.format("fabric:container-create --ensemble-server --url jclouds://aws-ec2?imageId=%s&locationId=%s&group=%s&user=%s --profile default ensemble1", image, location, group, user), 10 * 60000L, false));
        String publicIp = getNodePublicIp(computeService);
        assertNotNull(publicIp);
        Thread.sleep(DEFAULT_TIMEOUT);
        System.err.println(executeCommand("fabric:join -n " + publicIp + ":2181", 10 * 60000L, false));
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
            public boolean apply(@Nullable ComputeMetadata metadata) {
                NodeMetadata nodeMetadata = (NodeMetadata) metadata;
                return nodeMetadata.getGroup().equals(group) && nodeMetadata.getState().equals(NodeState.RUNNING);
            }
        })) {
            NodeMetadata nodeMetadata = (NodeMetadata) computeMetadata;
            return nodeMetadata.getPublicAddresses().toArray(new String[0])[0];

        }
        return null;
    }

    /**
     * Creates a security group that allows the current host to access ssh and zookeeper.
     *
     * @param port
     */
    private void setUpSecurityGroup(ComputeService computeService,int port) {
        if (computeService.getContext().getProviderSpecificContext().getApi() instanceof EC2Client) {
            EC2Client ec2Client = EC2Client.class.cast(computeService.getContext().getProviderSpecificContext().getApi());
            String groupName = "jclouds#" + group + "#" + location;

            try {
                ec2Client.getSecurityGroupServices().createSecurityGroupInRegion(location, groupName, "Fabric security group");
            } catch (Exception ex) {
                //Ignore
            }
            try {
                ec2Client.getSecurityGroupServices().authorizeSecurityGroupIngressInRegion(location, groupName, IpProtocol.TCP, port, port, getOriginatingIp());
            } catch (Exception ex) {
                //Ignore
            }
            try {
                ec2Client.getSecurityGroupServices().authorizeSecurityGroupIngressInRegion(location, groupName, IpProtocol.TCP, 22, 22, "0.0.0.0/0");
            } catch (Exception ex) {
                //Ignore
            }
        }
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
                copySystemProperty("fabricitest.aws.identity"),
                copySystemProperty("fabricitest.aws.credential"),
                copySystemProperty("fabricitest.aws.image"),
                copySystemProperty("fabricitest.aws.location"),
                copySystemProperty("fabricitest.aws.user"),
                editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.getArtifactVersion("org.fusesource.fabric","fuse-fabric")),
                editConfigurationFileExtend("etc/config.properties", "org.osgi.framework.executionenvironment", "JavaSE-1.7,JavaSE-1.6,JavaSE-1.5"),
                scanFeatures("jclouds","jclouds-compute").start()
        };
    }
}
