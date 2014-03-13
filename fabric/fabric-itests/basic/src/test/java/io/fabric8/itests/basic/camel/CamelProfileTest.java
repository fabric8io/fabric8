package io.fabric8.itests.basic.camel;

import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricFeaturesTest;

import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CamelProfileTest extends FabricFeaturesTest {

    @Test
    public void testFeatures() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();

            Set<ContainerProxy> containers = ContainerBuilder.create(fabricProxy).withName("feature-camel").withProfiles("feature-camel").assertProvisioningResult().build();
            try {
                CuratorFramework curator = fabricService.adapt(CuratorFramework.class);
                assertProvisionedFeature(fabricService, curator, containers, "camel-http", "feature-camel", "camel-http");
                assertProvisionedFeature(fabricService, curator, containers, "camel-jetty", "feature-camel", "camel-jetty");
                assertProvisionedFeature(fabricService, curator, containers, "camel-jms", "feature-camel", "camel-jms");
                assertProvisionedFeature(fabricService, curator, containers, "camel-ftp", "feature-camel", "camel-ftp");
                assertProvisionedFeature(fabricService, curator, containers, "camel-quartz", "feature-camel", "camel-quartz");
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}
