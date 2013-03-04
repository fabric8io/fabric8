package org.fusesource.fabric.itests.paxexam.esb;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.itests.paxexam.FabricFeaturesTest;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.util.Set;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class EsbProfileLongTest extends FabricFeaturesTest {

    @Before
    public void setUp() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));

        Set<Container> containers = ContainerBuilder.create().withName("esb").withProfiles("jboss-fuse-minimal").assertProvisioningResult().build();
        for (Container container : containers) {
            prepareFeaturesForTesting(container.getId(), "connector", "jboss-fuse-minimal", "geronimo-connector");
            prepareFeaturesForTesting(container.getId(), "saaj", "jboss-fuse-minimal", "saaj-impl");
            prepareFeaturesForTesting(container.getId(), "cxf-osgi", "jboss-fuse-minimal", "org.apache.cxf.bundle");
            prepareFeaturesForTesting(container.getId(), "cxf-jaxrs", "jboss-fuse-minimal", "jettison");
            prepareFeaturesForTesting(container.getId(), "cxf-nmr", "jboss-fuse-minimal", "org.apache.servicemix.cxf.binding.nmr");
            prepareFeaturesForTesting(container.getId(), "camel-nmr", "jboss-fuse-minimal", "org.apache.servicemix.camel.component");
            prepareFeaturesForTesting(container.getId(), "camel-activemq", "jboss-fuse-minimal", "activemq-camel");
            prepareFeaturesForTesting(container.getId(), "examples-cxf-osgi", "jboss-fuse-minimal", "cxf-osgi");
            prepareFeaturesForTesting(container.getId(), "examples-cxf-jaxrs", "jboss-fuse-minimal", "cxf-jaxrs");
            prepareFeaturesForTesting(container.getId(), "examples-cxf-nmr", "jboss-fuse-minimal", "cxf-nmr");
            prepareFeaturesForTesting(container.getId(), "examples-camel-osgi", "jboss-fuse-minimal", "camel-osgi");
            prepareFeaturesForTesting(container.getId(), "examples-camel-blueprint", "jboss-fuse-minimal", "camel-blueprint");
            prepareFeaturesForTesting(container.getId(), "examples-camel-nmr", "jboss-fuse-minimal", "camel-nmr");
            prepareFeaturesForTesting(container.getId(), "examples-camel-nmr-blueprint", "jboss-fuse-minimal", "camel-nmr-blueprint");
            prepareFeaturesForTesting(container.getId(), "examples-cxf-camel-nmr", "jboss-fuse-minimal", "cxf-camel-nmr");
            prepareFeaturesForTesting(container.getId(), "examples-cxf-ws-addressing", "jboss-fuse-minimal", "cxf-ws-addressing");
            prepareFeaturesForTesting(container.getId(), "examples-cxf-wsdl-first-osgi-package", "jboss-fuse-minimal", "cxf-wsdl-first-osgi-package");
            prepareFeaturesForTesting(container.getId(), "examples-cxf-ws-security-osgi", "jboss-fuse-minimal", "cxf-ws-security-osgi");
            prepareFeaturesForTesting(container.getId(), "jpa-hibernate", "jboss-fuse-minimal", "jpa-hibernate");
            prepareFeaturesForTesting(container.getId(), "examples-jpa-osgi", "jboss-fuse-minimal", "jpa-osgi");
            prepareFeaturesForTesting(container.getId(), "examples-cxf-ws-rm", "jboss-fuse-minimal", "cxf-ws-rm");
            prepareFeaturesForTesting(container.getId(), "servicemix-shared", "jboss-fuse-minimal", "servicemix-shared");
            prepareFeaturesForTesting(container.getId(), "servicemix-cxf-bc", "jboss-fuse-minimal", "servicemix-cxf-bc");
            prepareFeaturesForTesting(container.getId(), "servicemix-file", "jboss-fuse-minimal", "servicemix-file");
            prepareFeaturesForTesting(container.getId(), "servicemix-ftp", "jboss-fuse-minimal", "servicemix-ftp");
            prepareFeaturesForTesting(container.getId(), "servicemix-http", "jboss-fuse-minimal", "servicemix-http");
            prepareFeaturesForTesting(container.getId(), "servicemix-jms", "jboss-fuse-minimal", "servicemix-jms");
            prepareFeaturesForTesting(container.getId(), "servicemix-mail", "jboss-fuse-minimal", "servicemix-mail");
            prepareFeaturesForTesting(container.getId(), "servicemix-bean", "jboss-fuse-minimal", "servicemix-bean");
            prepareFeaturesForTesting(container.getId(), "servicemix-camel", "jboss-fuse-minimal", "servicemix-camel");
            prepareFeaturesForTesting(container.getId(), "servicemix-drools", "jboss-fuse-minimal", "servicemix-drools");
            prepareFeaturesForTesting(container.getId(), "servicemix-cxf-se", "jboss-fuse-minimal", "servicemix-cxf-se");
            prepareFeaturesForTesting(container.getId(), "servicemix-eip", "jboss-fuse-minimal", "servicemix-eip");
            prepareFeaturesForTesting(container.getId(), "servicemix-osworkflow", "jboss-fuse-minimal", "servicemix-osworkflow");
            prepareFeaturesForTesting(container.getId(), "servicemix-quartz", "jboss-fuse-minimal", "servicemix-quartz");
            prepareFeaturesForTesting(container.getId(), "servicemix-scripting", "jboss-fuse-minimal", "servicemix-scripting");
            prepareFeaturesForTesting(container.getId(), "servicemix-validation", "jboss-fuse-minimal", "servicemix-validation");
            prepareFeaturesForTesting(container.getId(), "servicemix-saxon", "jboss-fuse-minimal", "servicemix-saxon");
            prepareFeaturesForTesting(container.getId(), "servicemix-wsn2005", "jboss-fuse-minimal", "servicemix-wsn2005");
            prepareFeaturesForTesting(container.getId(), "servicemix-snmp", "jboss-fuse-minimal", "servicemix-snmp");
            prepareFeaturesForTesting(container.getId(), "servicemix-vfs", "jboss-fuse-minimal", "servicemix-vfs");
            prepareFeaturesForTesting(container.getId(), "servicemix-smpp", "jboss-fuse-minimal", "servicemix-smpp");
            prepareFeaturesForTesting(container.getId(), "activemq-broker", "jboss-fuse-minimal", "activemq-broker");
        }
    }

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }
}
