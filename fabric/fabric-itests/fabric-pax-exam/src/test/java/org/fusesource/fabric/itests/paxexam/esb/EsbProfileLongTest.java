package org.fusesource.fabric.itests.paxexam.esb;

import org.fusesource.fabric.itests.paxexam.FabricFeaturesTest;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class EsbProfileLongTest extends FabricFeaturesTest {

    @Before
    public void setUp() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));

        addStagingRepoToDefaultProfile();

        createAndAssertChildContainer("esb1", "root", "fuse-esb-minimal");
        prepareFeaturesForTesting("esb1", "connector", "fuse-esb-minimal", "geronimo-connector");
        prepareFeaturesForTesting("esb1", "saaj", "fuse-esb-minimal", "saaj-impl");
        prepareFeaturesForTesting("esb1", "cxf-osgi", "fuse-esb-minimal", "org.apache.cxf.bundle");
        prepareFeaturesForTesting("esb1", "cxf-jaxrs", "fuse-esb-minimal", "jettison");
        prepareFeaturesForTesting("esb1", "cxf-nmr", "fuse-esb-minimal", "org.apache.servicemix.cxf.binding.nmr");
        prepareFeaturesForTesting("esb1", "camel-nmr", "fuse-esb-minimal", "org.apache.servicemix.camel.component");
        prepareFeaturesForTesting("esb1", "camel-activemq", "fuse-esb-minimal", "activemq-camel");
        prepareFeaturesForTesting("esb1", "examples-cxf-osgi", "fuse-esb-minimal", "cxf-osgi");
        prepareFeaturesForTesting("esb1", "examples-cxf-jaxrs", "fuse-esb-minimal", "cxf-jaxrs");
        prepareFeaturesForTesting("esb1", "examples-cxf-nmr", "fuse-esb-minimal", "cxf-nmr");
        prepareFeaturesForTesting("esb1", "examples-camel-osgi", "fuse-esb-minimal", "camel-osgi");
        prepareFeaturesForTesting("esb1", "examples-camel-blueprint", "fuse-esb-minimal", "camel-blueprint");
        prepareFeaturesForTesting("esb1", "examples-camel-nmr", "fuse-esb-minimal", "camel-nmr");
        prepareFeaturesForTesting("esb1", "examples-camel-nmr-blueprint", "fuse-esb-minimal", "camel-nmr-blueprint");
        prepareFeaturesForTesting("esb1", "examples-cxf-camel-nmr", "fuse-esb-minimal", "cxf-camel-nmr");
        prepareFeaturesForTesting("esb1", "examples-cxf-ws-addressing", "fuse-esb-minimal", "cxf-ws-addressing");
        prepareFeaturesForTesting("esb1", "examples-cxf-wsdl-first-osgi-package", "fuse-esb-minimal", "cxf-wsdl-first-osgi-package");
        prepareFeaturesForTesting("esb1", "examples-cxf-ws-security-osgi", "fuse-esb-minimal", "cxf-ws-security-osgi");
        prepareFeaturesForTesting("esb1", "jpa-hibernate", "fuse-esb-minimal", "jpa-hibernate");
        prepareFeaturesForTesting("esb1", "examples-jpa-osgi", "fuse-esb-minimal", "jpa-osgi");
        prepareFeaturesForTesting("esb1", "examples-cxf-ws-rm", "fuse-esb-minimal", "cxf-ws-rm");
        prepareFeaturesForTesting("esb1", "servicemix-shared", "fuse-esb-minimal", "servicemix-shared");
        prepareFeaturesForTesting("esb1", "servicemix-cxf-bc", "fuse-esb-minimal", "servicemix-cxf-bc");
        prepareFeaturesForTesting("esb1", "servicemix-file", "fuse-esb-minimal", "servicemix-file");
        prepareFeaturesForTesting("esb1", "servicemix-ftp", "fuse-esb-minimal", "servicemix-ftp");
        prepareFeaturesForTesting("esb1", "servicemix-http", "fuse-esb-minimal", "servicemix-http");
        prepareFeaturesForTesting("esb1", "servicemix-jms", "fuse-esb-minimal", "servicemix-jms");
        prepareFeaturesForTesting("esb1", "servicemix-mail", "fuse-esb-minimal", "servicemix-mail");
        prepareFeaturesForTesting("esb1", "servicemix-bean", "fuse-esb-minimal", "servicemix-bean");
        prepareFeaturesForTesting("esb1", "servicemix-camel", "fuse-esb-minimal", "servicemix-camel");
        prepareFeaturesForTesting("esb1", "servicemix-drools", "fuse-esb-minimal", "servicemix-drools");
        prepareFeaturesForTesting("esb1", "servicemix-cxf-se", "fuse-esb-minimal", "servicemix-cxf-se");
        prepareFeaturesForTesting("esb1", "servicemix-eip", "fuse-esb-minimal", "servicemix-eip");
        prepareFeaturesForTesting("esb1", "servicemix-osworkflow", "fuse-esb-minimal", "servicemix-osworkflow");
        prepareFeaturesForTesting("esb1", "servicemix-quartz", "fuse-esb-minimal", "servicemix-quartz");
        prepareFeaturesForTesting("esb1", "servicemix-scripting", "fuse-esb-minimal", "servicemix-scripting");
        prepareFeaturesForTesting("esb1", "servicemix-validation", "fuse-esb-minimal", "servicemix-validation");
        prepareFeaturesForTesting("esb1", "servicemix-saxon", "fuse-esb-minimal", "servicemix-saxon");
        prepareFeaturesForTesting("esb1", "servicemix-wsn2005", "fuse-esb-minimal", "servicemix-wsn2005");
        prepareFeaturesForTesting("esb1", "servicemix-snmp", "fuse-esb-minimal", "servicemix-snmp");
        prepareFeaturesForTesting("esb1", "servicemix-vfs", "fuse-esb-minimal", "servicemix-vfs");
        prepareFeaturesForTesting("esb1", "servicemix-smpp", "fuse-esb-minimal", "servicemix-smpp");
        prepareFeaturesForTesting("esb1", "activemq-broker", "fuse-esb-minimal", "activemq-broker");
    }
    
    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("esb1");
    }
}
