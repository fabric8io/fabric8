package io.fabric8.jolokia.facade;

import io.fabric8.api.*;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;

/**
 * @author Stan Lewis
 */
@Ignore("[FABRIC-783][7.4] Fix jolokia FabricServiceFacadeTest")
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
    public void testGetFabricStatus() {

        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricService service = getFabricService();

        System.out.println("Grabbing Fabric Status...");

        FabricStatus status = service.getFabricStatus();

        System.out.println("Fabric Status: " + status);
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
    public void testProfileRefresh() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));
        FabricService service = getFabricService();

        Profile p = service.getVersion("1.0").getProfile("fabric");
        p.refresh();
    }


    @Test
    public void testCreateChildContainer() throws InterruptedException {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricService service = getFabricService();

        CreateChildContainerOptions options = CreateChildContainerOptions.builder().jmxUser(userName).jmxPassword(password).name("test").parent("root").build();

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

    @Test
    public void testGetProfilesOfVersion() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricService service = getFabricService();

        Version v = service.getDefaultVersion();
        Assume.assumeNotNull(v);
        Profile[] profs = v.getProfiles();
        Assume.assumeNotNull(profs);
        for (Profile p : profs) {
            System.out.println(p.getId() + " - " + p.getVersion());
        }
    }

    @Test
    public void testParentsOfProfile() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricService service = getFabricService();

        Version v = service.getDefaultVersion();
        Assume.assumeNotNull(v);
        Profile prof = v.getProfile("hawtio");
        Assume.assumeNotNull(prof);
        Profile[] parents = prof.getParents();

        for (Profile p : parents) {
            System.out.println(p.getId() + " - " + p.getVersion());
        }
    }

    @Test
    public void testConfigurationModifications() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        final String version = "1.0";
        final String profile = "hawtio";
        final String pid = "io.fabric8.agent.properties";
        final String key = "feature.fabric-hawtio";
        final String origVal = "fabric-hawtio";
        final String testVal = "test1";

        FabricService service = getFabricService();

        String val = service.getConfigurationValue(version, profile, pid, key);
        System.out.println("Original Value: " + val);
        Assume.assumeNotNull(val);
        Assume.assumeTrue(origVal.equals(val));

        service.setConfigurationValue(version, profile, pid, key, testVal);
        String val2 = service.getConfigurationValue(version, profile, pid, key);
        System.out.println("New Value: " + val2);
        Assume.assumeNotNull(val2);
        Assume.assumeTrue(testVal.equals(val2));

        service.setConfigurationValue(version, profile, pid, key, origVal);
        val2 = service.getConfigurationValue(version, profile, pid, key);
        System.out.println("Restored Value: " + val2);
        Assume.assumeNotNull(val2);
        Assume.assumeTrue(origVal.equals(val2));
    }
}
