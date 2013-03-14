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
            prepareFeaturesForTesting(containers, "connector", "jboss-fuse-minimal", "geronimo-connector");
            prepareFeaturesForTesting(containers, "saaj", "jboss-fuse-minimal", "saaj-impl");
            prepareFeaturesForTesting(containers, "cxf-osgi", "jboss-fuse-minimal", "org.apache.cxf.bundle");
            prepareFeaturesForTesting(containers, "cxf-jaxrs", "jboss-fuse-minimal", "jettison");
            prepareFeaturesForTesting(containers, "cxf-nmr", "jboss-fuse-minimal", "org.apache.servicemix.cxf.binding.nmr");
            prepareFeaturesForTesting(containers, "camel-nmr", "jboss-fuse-minimal", "org.apache.servicemix.camel.component");
            prepareFeaturesForTesting(containers, "camel-activemq", "jboss-fuse-minimal", "activemq-camel");
            prepareFeaturesForTesting(containers, "examples-cxf-osgi", "jboss-fuse-minimal", "cxf-osgi");
            prepareFeaturesForTesting(containers, "examples-cxf-jaxrs", "jboss-fuse-minimal", "cxf-jaxrs");
            prepareFeaturesForTesting(containers, "examples-cxf-nmr", "jboss-fuse-minimal", "cxf-nmr");
            prepareFeaturesForTesting(containers, "examples-camel-osgi", "jboss-fuse-minimal", "camel-osgi");
            prepareFeaturesForTesting(containers, "examples-camel-blueprint", "jboss-fuse-minimal", "camel-blueprint");
            prepareFeaturesForTesting(containers, "examples-camel-nmr", "jboss-fuse-minimal", "camel-nmr");
            prepareFeaturesForTesting(containers, "examples-camel-nmr-blueprint", "jboss-fuse-minimal", "camel-nmr-blueprint");
            prepareFeaturesForTesting(containers, "examples-cxf-camel-nmr", "jboss-fuse-minimal", "cxf-camel-nmr");
            prepareFeaturesForTesting(containers, "examples-cxf-ws-addressing", "jboss-fuse-minimal", "cxf-ws-addressing");
            prepareFeaturesForTesting(containers, "examples-cxf-wsdl-first-osgi-package", "jboss-fuse-minimal", "cxf-wsdl-first-osgi-package");
            prepareFeaturesForTesting(containers, "examples-cxf-ws-security-osgi", "jboss-fuse-minimal", "cxf-ws-security-osgi");
            prepareFeaturesForTesting(containers, "jpa-hibernate", "jboss-fuse-minimal", "jpa-hibernate");
            prepareFeaturesForTesting(containers, "examples-jpa-osgi", "jboss-fuse-minimal", "jpa-osgi");
            prepareFeaturesForTesting(containers, "examples-cxf-ws-rm", "jboss-fuse-minimal", "cxf-ws-rm");
            prepareFeaturesForTesting(containers, "servicemix-shared", "jboss-fuse-minimal", "servicemix-shared");
            prepareFeaturesForTesting(containers, "servicemix-cxf-bc", "jboss-fuse-minimal", "servicemix-cxf-bc");
            prepareFeaturesForTesting(containers, "servicemix-file", "jboss-fuse-minimal", "servicemix-file");
            prepareFeaturesForTesting(containers, "servicemix-ftp", "jboss-fuse-minimal", "servicemix-ftp");
            prepareFeaturesForTesting(containers, "servicemix-http", "jboss-fuse-minimal", "servicemix-http");
            prepareFeaturesForTesting(containers, "servicemix-jms", "jboss-fuse-minimal", "servicemix-jms");
            prepareFeaturesForTesting(containers, "servicemix-mail", "jboss-fuse-minimal", "servicemix-mail");
            prepareFeaturesForTesting(containers, "servicemix-bean", "jboss-fuse-minimal", "servicemix-bean");
            prepareFeaturesForTesting(containers, "servicemix-camel", "jboss-fuse-minimal", "servicemix-camel");
            prepareFeaturesForTesting(containers, "servicemix-drools", "jboss-fuse-minimal", "servicemix-drools");
            prepareFeaturesForTesting(containers, "servicemix-cxf-se", "jboss-fuse-minimal", "servicemix-cxf-se");
            prepareFeaturesForTesting(containers, "servicemix-eip", "jboss-fuse-minimal", "servicemix-eip");
            prepareFeaturesForTesting(containers, "servicemix-osworkflow", "jboss-fuse-minimal", "servicemix-osworkflow");
            prepareFeaturesForTesting(containers, "servicemix-quartz", "jboss-fuse-minimal", "servicemix-quartz");
            prepareFeaturesForTesting(containers, "servicemix-scripting", "jboss-fuse-minimal", "servicemix-scripting");
            prepareFeaturesForTesting(containers, "servicemix-validation", "jboss-fuse-minimal", "servicemix-validation");
            prepareFeaturesForTesting(containers, "servicemix-saxon", "jboss-fuse-minimal", "servicemix-saxon");
            prepareFeaturesForTesting(containers, "servicemix-wsn2005", "jboss-fuse-minimal", "servicemix-wsn2005");
            prepareFeaturesForTesting(containers, "servicemix-snmp", "jboss-fuse-minimal", "servicemix-snmp");
            prepareFeaturesForTesting(containers, "servicemix-vfs", "jboss-fuse-minimal", "servicemix-vfs");
            prepareFeaturesForTesting(containers, "servicemix-smpp", "jboss-fuse-minimal", "servicemix-smpp");
            prepareFeaturesForTesting(containers, "activemq-broker", "jboss-fuse-minimal", "activemq-broker");
    }

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }
}
