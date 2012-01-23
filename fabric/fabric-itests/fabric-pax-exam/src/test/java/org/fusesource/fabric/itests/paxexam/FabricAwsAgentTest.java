/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.itests.paxexam;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.base.Predicate;
import org.apache.commons.io.IOUtils;
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
import org.linkedin.zookeeper.client.IZKClient;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricAwsAgentTest extends FabricCommandsTestSupport {

    private String identity;
    private String credential;
    private String image;
    private String location;
    private String user;
    private String group = "fabricitests";

    public boolean isReady() {
        return identity != null && credential != null && image != null & user != null;
    }

    @Before
    public void setUp() {
        identity = System.getProperty("aws.identity");
        credential = System.getProperty("aws.credential");
        image = System.getProperty("aws.image");
        location = System.getProperty("aws.location");
        user = System.getProperty("aws.user");
    }


    @After
    public void tearDown() {
        System.err.println(executeCommand("group-destroy " + group, 30000L, false));
    }

    @Test
    public void testAwsAgentCreation() throws InterruptedException, IOException {
        if (!isReady()) {
            System.err.println("Amazon is not setup correctly. This test will not run.");
            System.err.println("To prpoerly run this test, you need to setup with maven the following properties:");
            System.err.println("aws.identity \t The aws access id");
            System.err.println("aws.credential \t The aws access key");
            System.err.println("aws.image  \t The aws (java ready) image");
            System.err.println("aws.group  \t The aws group. It needs to have in advance configured firewall for zookeeper access");
            System.err.println("aws.user  \t The user under which the agent will run");
            return;
        }

        System.err.println(executeCommand("features:install jclouds-aws-ec2 fabric-jclouds jclouds-commands"));

        //Filtering out regions because there is a temporary connectivity issue with us-west-2.
        executeCommands("config:edit org.jclouds.compute-ec2",
                "config:propset provider aws-ec2 ",
                "config:propset identity " + identity,
                "config:propset credential " + credential,
                "config:propset jclouds.regions eu-west-1,us-west-1,us-east-1",
                "config:update");

        setUpSecurityGroup(2181);
        Thread.sleep(3 * DEFAULT_TIMEOUT);
        System.err.println(executeCommand(String.format("fabric:agent-create --ensemble-server --url jclouds://aws-ec2?imageId=%s&locationId=%s&group=%s&user=%s --profile default ensemble1", image, location, group, user), 10 * 60000L, false));
        String publicIp = getNodePublicIp();
        assertNotNull(publicIp);
        System.err.println(executeCommand("fabric:join " + publicIp + ":2181", 10 * 60000L, false));
        String agentList = executeCommand("fabric:agent-list");
        assertTrue(agentList.contains("root") && agentList.contains("ensemble1"));

    }

    /**
     * Return the public ip of the generated node.
     *
     * @return
     */
    private String getNodePublicIp() {
        ComputeService computeService = getOsgiService(ComputeService.class, 3*DEFAULT_TIMEOUT);
        Set s;
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
    private void setUpSecurityGroup(int port) {
        ComputeService computeService = getOsgiService(ComputeService.class);
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
                fabricDistributionConfiguration(), keepRuntimeFolder(), logLevel(LogLevelOption.LogLevel.ERROR),
                editConfigurationFileExtend("etc/system.properties", "aws.identity", System.getProperty("aws.identity")),
                editConfigurationFileExtend("etc/system.properties", "aws.credential", System.getProperty("aws.credential")),
                editConfigurationFileExtend("etc/system.properties", "aws.image", System.getProperty("aws.image")),
                editConfigurationFileExtend("etc/system.properties", "aws.location", System.getProperty("aws.location")),
                editConfigurationFileExtend("etc/system.properties", "aws.user", System.getProperty("aws.user")),
                editConfigurationFileExtend("etc/config.properties", "org.osgi.framework.executionenvironment", "JavaSE-1.7,JavaSE-1.6,JavaSE-1.5"),
                scanFeatures("jclouds","jclouds-compute").start(),

                debugConfiguration("5005", true)};
    }
}
