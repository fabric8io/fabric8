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

        createAndAssertChildContainer("esb1", "root", "esb");
        prepareFeaturesForTesting("esb1", "connector", "esb", "geronimo-connector");
        prepareFeaturesForTesting("esb1", "saaj", "esb", "saaj-impl");
        prepareFeaturesForTesting("esb1", "cxf-osgi", "esb", "org.apache.cxf.bundle");
        prepareFeaturesForTesting("esb1", "cxf-jaxrs", "esb", "jettison");
        prepareFeaturesForTesting("esb1", "cxf-nmr", "esb", "org.apache.servicemix.cxf.binding.nmr");
        prepareFeaturesForTesting("esb1", "camel-nmr", "esb", "org.apache.servicemix.camel.component");
        prepareFeaturesForTesting("esb1", "camel-activemq", "esb", "activemq-camel");
        prepareFeaturesForTesting("esb1", "examples-cxf-osgi", "esb", "cxf-osgi");
        prepareFeaturesForTesting("esb1", "examples-cxf-jaxrs", "esb", "cxf-jaxrs");
        prepareFeaturesForTesting("esb1", "examples-cxf-nmr", "esb", "cxf-nmr");
        prepareFeaturesForTesting("esb1", "examples-camel-osgi", "esb", "camel-osgi");
        prepareFeaturesForTesting("esb1", "examples-camel-blueprint", "esb", "camel-blueprint");
        prepareFeaturesForTesting("esb1", "examples-camel-nmr", "esb", "camel-nmr");
        prepareFeaturesForTesting("esb1", "examples-camel-nmr-blueprint", "esb", "camel-nmr-blueprint");
        prepareFeaturesForTesting("esb1", "examples-cxf-camel-nmr", "esb", "cxf-camel-nmr");
        prepareFeaturesForTesting("esb1", "examples-cxf-ws-addressing", "esb", "cxf-ws-addressing");
        prepareFeaturesForTesting("esb1", "examples-cxf-wsdl-first-osgi-package", "esb", "cxf-wsdl-first-osgi-package");
        prepareFeaturesForTesting("esb1", "examples-cxf-ws-security-osgi", "esb", "cxf-ws-security-osgi");
        prepareFeaturesForTesting("esb1", "jpa-hibernate", "esb", "jpa-hibernate");
        prepareFeaturesForTesting("esb1", "examples-jpa-osgi", "esb", "jpa-osgi");
        prepareFeaturesForTesting("esb1", "examples-cxf-ws-rm", "esb", "cxf-ws-rm");
        prepareFeaturesForTesting("esb1", "servicemix-shared", "esb", "servicemix-shared");
        prepareFeaturesForTesting("esb1", "servicemix-cxf-bc", "esb", "servicemix-cxf-bc");
        prepareFeaturesForTesting("esb1", "servicemix-file", "esb", "servicemix-file");
        prepareFeaturesForTesting("esb1", "servicemix-ftp", "esb", "servicemix-ftp");
        prepareFeaturesForTesting("esb1", "servicemix-http", "esb", "servicemix-http");
        prepareFeaturesForTesting("esb1", "servicemix-jms", "esb", "servicemix-jms");
        prepareFeaturesForTesting("esb1", "servicemix-mail", "esb", "servicemix-mail");
        prepareFeaturesForTesting("esb1", "servicemix-bean", "esb", "servicemix-bean");
        prepareFeaturesForTesting("esb1", "servicemix-camel", "esb", "servicemix-camel");
        prepareFeaturesForTesting("esb1", "servicemix-drools", "esb", "servicemix-drools");
        prepareFeaturesForTesting("esb1", "servicemix-cxf-se", "esb", "servicemix-cxf-se");
        prepareFeaturesForTesting("esb1", "servicemix-eip", "esb", "servicemix-eip");
        prepareFeaturesForTesting("esb1", "servicemix-osworkflow", "esb", "servicemix-osworkflow");
        prepareFeaturesForTesting("esb1", "servicemix-quartz", "esb", "servicemix-quartz");
        prepareFeaturesForTesting("esb1", "servicemix-scripting", "esb", "servicemix-scripting");
        prepareFeaturesForTesting("esb1", "servicemix-validation", "esb", "servicemix-validation");
        prepareFeaturesForTesting("esb1", "servicemix-saxon", "esb", "servicemix-saxon");
        prepareFeaturesForTesting("esb1", "servicemix-wsn2005", "esb", "servicemix-wsn2005");
        prepareFeaturesForTesting("esb1", "servicemix-snmp", "esb", "servicemix-snmp");
        prepareFeaturesForTesting("esb1", "servicemix-vfs", "esb", "servicemix-vfs");
        prepareFeaturesForTesting("esb1", "servicemix-smpp", "esb", "servicemix-smpp");
        prepareFeaturesForTesting("esb1", "activemq-broker", "esb", "activemq-broker");
    }
    
    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("esb1");
    }
}
