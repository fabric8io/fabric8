package org.fusesource.fabric.jolokia.facade;

import org.fusesource.fabric.api.*;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

/**
 * @author Stan Lewis
 */
public class FabricServiceFacadeTest {

    String userName = System.getProperty("fabric.user", "admin");
    String password = System.getProperty("fabric.password", "admin");

    // note, make sure there's an ending /, jolokia doesn't handle HTTP 302 too well...
    String url = System.getProperty("fabric.url", "http://localhost:8181/jolokia/");


    @Before
    public void maybeEnableLogging() {
        if (Boolean.valueOf(System.getProperty("logging"))) {
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
            System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
            System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "DEBUG");
        }
    }

    public FabricService getFabricService() {
        JolokiaFabricConnector connector = JolokiaFabricConnector.getFabricConnector(userName, password, url);
        FabricService service = connector.getFabricServiceFacade();
        return service;
    }

    @Test
    public void testGetContainer() {

        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricService service = getFabricService();

        Container[] containers = service.getContainers();

        for (Container container : containers) {
            System.out.println("container: " + container.getId());
            System.out.println("alive: " + container.isAlive());
            System.out.println("type: " + container.getType());
            System.out.println("ensembleServer: " + container.isEnsembleServer());
            System.out.println("JMX domains: " + container.getJmxDomains());
            System.out.println("Provision status: " + container.getProvisionStatus());
            //System.out.println("Provision list: " + container.getProvisionList());
        }
    }

    @Test
    public void testGetContainerMetadata() {

        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricService service = getFabricService();

        Container[] containers = service.getContainers();

        for (Container container : containers) {
            System.out.println("container: " + container.getMetadata());
        }
    }


    @Test
    public void testGetProfile() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricService service = getFabricService();
        Version one_dot_oh = service.getVersion("1.0");
        Profile _default = one_dot_oh.getProfile("default");

        System.out.println("Default bundles: " + _default.getBundles());
        System.out.println("Default features: " + _default.getFeatures());
        System.out.println("Default fabs: " + _default.getFabs());
        System.out.println("Default repos: " + _default.getRepositories());
    }

    @Test
    public void testGetChildContainers() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricService service = getFabricService();
        Container[] containers = service.getContainers();

        for (Container container : containers) {
            Container[] children = container.getChildren();
            for (Container child : children) {
                System.out.println("Child: " + child.getId() + " alive: " + child.isAlive());
            }
        }

    }

    @Test
    public void testCreateChildContainer() throws InterruptedException {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricService service = getFabricService();

        CreateChildContainerOptions options = CreateContainerOptionsBuilder.child().jmxUser(userName).jmxPassword(password).name("test").parent("root").number(1).build();

        service.createContainers(options);

        System.out.println("Sleeping...");

        Thread.sleep(10000);

        Container test = service.getContainer("test");
        if (test.isAlive()) {
            test.destroy();
        }

    }

    @Test
    public void testSomeOtherFabricMethods() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricService service = getFabricService();

        Container container = service.getCurrentContainer();
        System.out.println("Current container: " + container.getId());

        URI repoUri = service.getMavenRepoURI();
        System.out.println("Repo URI: " + repoUri);

        String zookeeperUrl = service.getZookeeperUrl();
        System.out.println("zookeeper URL: " + zookeeperUrl);

    }

    @Test
    public void testCreatingVersion() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricService service = getFabricService();

        Version version = service.createVersion("1.5");
        service.setDefaultVersion(version);
        Version one_dot_oh = service.getVersion("1.0");
        service.setDefaultVersion(one_dot_oh);
        version.delete();

    }



}
