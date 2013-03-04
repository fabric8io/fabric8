package org.fusesource.fabric.itests.paxexam.camel;


import com.google.inject.Inject;
import junit.framework.Assert;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.itests.paxexam.FabricTestSupport;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.jansi.AnsiString;
import org.junit.After;
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
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricCamelComponentTest extends FabricTestSupport {


    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("server2");
        destroyChildContainer("client1");
        destroyChildContainer("server1");
    }

    @Test
    public void testRegistryEntries() throws Exception {
        System.err.println(executeCommand("fabric:create -n root"));
        //Wait for zookeeper service to become available.
        executeCommand("fabric:profile-create --parents camel fabric-camel");
        executeCommand("fabric:profile-create --parents fabric-camel fabric-camel-server");
        executeCommand("fabric:profile-create --parents fabric-camel-server fabric-camel-server-1");
        executeCommand("fabric:profile-create --parents fabric-camel-server fabric-camel-server-2");
        executeCommand("fabric:profile-create --parents fabric-camel fabric-camel-client");
        executeCommand("fabric:profile-edit --repositories mvn:org.fusesource.examples.fabric-camel-cluster/features/" + System.getProperty("fabric.version") + "/xml/features fabric-camel");

        executeCommand("fabric:profile-edit --features camel-server fabric-camel-server");
        executeCommand("fabric:profile-edit --features camel-client fabric-camel-client");

        executeCommand("fabric:profile-edit --pid org.fusesource.fabric.examples.camel.loadbalancing.server/portNumber=9191 fabric-camel-server-1");
        executeCommand("fabric:profile-edit --pid org.fusesource.fabric.examples.camel.loadbalancing.server/portNumber=9292 fabric-camel-server-2");

        createAndAssertChildContainer("server1", "root", "fabric-camel-server-1", "-Xms512m -Xmx512m");
        createAndAssertChildContainer("server2", "root", "fabric-camel-server-2", "-Xms512m -Xmx512m");
        createAndAssertChildContainer("client1", "root", "fabric-camel-client", "-Xms512m -Xmx512m -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005");
        //Check that the entries have been properly propagated.
        Assert.assertNotNull(getZookeeper().exists("/fabric/registry/camel/endpoints/"));
        Assert.assertEquals(1, getZookeeper().getChildren("/fabric/registry/camel/endpoints/").size());
        Thread.sleep(5000);
        System.err.println(executeCommand("fabric:container-connect -u admin -p admin client1 camel:route-list"));
        String response = new AnsiString(executeCommand("fabric:container-connect -u admin -p admin client1 camel:route-info fabric-client | grep Failed")).getPlain().toString();
        System.err.println(response);
        int failed = Integer.parseInt(response.replaceAll("[^0-9]", ""));
        Assert.assertEquals(0, failed);
        destroyChildContainer("server1");

        Thread.sleep(25000);
        response = new AnsiString(executeCommand("fabric:container-connect -u admin -p admin client1 camel:route-info fabric-client | grep Failed")).getPlain().toString();
        System.err.println(response);

        failed = Integer.parseInt(response.replaceAll("[^0-9]", ""));
        Assert.assertEquals(0, failed);
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                //debugConfiguration("5005",false),
                editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.asInProject().getVersion(GROUP_ID, ARTIFACT_ID))
        };
    }
}
