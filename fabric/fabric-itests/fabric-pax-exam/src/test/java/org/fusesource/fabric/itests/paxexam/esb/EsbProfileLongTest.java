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

        createAndAssertChildContainer("esb1", "root", "jboss-fuse-minimal");
        prepareFeaturesForTesting("esb1", "connector", "jboss-fuse-minimal", "geronimo-connector");
        prepareFeaturesForTesting("esb1", "saaj", "jboss-fuse-minimal", "saaj-impl");
        prepareFeaturesForTesting("esb1", "cxf-osgi", "jboss-fuse-minimal", "org.apache.cxf.bundle");
        prepareFeaturesForTesting("esb1", "cxf-jaxrs", "jboss-fuse-minimal", "jettison");
        prepareFeaturesForTesting("esb1", "cxf-nmr", "jboss-fuse-minimal", "org.apache.servicemix.cxf.binding.nmr");
        prepareFeaturesForTesting("esb1", "camel-nmr", "jboss-fuse-minimal", "org.apache.servicemix.camel.component");
        prepareFeaturesForTesting("esb1", "camel-activemq", "jboss-fuse-minimal", "activemq-camel");
        prepareFeaturesForTesting("esb1", "examples-cxf-osgi", "jboss-fuse-minimal", "cxf-osgi");
        prepareFeaturesForTesting("esb1", "examples-cxf-jaxrs", "jboss-fuse-minimal", "cxf-jaxrs");
        prepareFeaturesForTesting("esb1", "examples-cxf-nmr", "jboss-fuse-minimal", "cxf-nmr");
        prepareFeaturesForTesting("esb1", "examples-camel-osgi", "jboss-fuse-minimal", "camel-osgi");
        prepareFeaturesForTesting("esb1", "examples-camel-blueprint", "jboss-fuse-minimal", "camel-blueprint");
        prepareFeaturesForTesting("esb1", "examples-camel-nmr", "jboss-fuse-minimal", "camel-nmr");
        prepareFeaturesForTesting("esb1", "examples-camel-nmr-blueprint", "jboss-fuse-minimal", "camel-nmr-blueprint");
        prepareFeaturesForTesting("esb1", "examples-cxf-camel-nmr", "jboss-fuse-minimal", "cxf-camel-nmr");
        prepareFeaturesForTesting("esb1", "examples-cxf-ws-addressing", "jboss-fuse-minimal", "cxf-ws-addressing");
        prepareFeaturesForTesting("esb1", "examples-cxf-wsdl-first-osgi-package", "jboss-fuse-minimal", "cxf-wsdl-first-osgi-package");
        prepareFeaturesForTesting("esb1", "examples-cxf-ws-security-osgi", "jboss-fuse-minimal", "cxf-ws-security-osgi");
        prepareFeaturesForTesting("esb1", "jpa-hibernate", "jboss-fuse-minimal", "jpa-hibernate");
        prepareFeaturesForTesting("esb1", "examples-jpa-osgi", "jboss-fuse-minimal", "jpa-osgi");
        prepareFeaturesForTesting("esb1", "examples-cxf-ws-rm", "jboss-fuse-minimal", "cxf-ws-rm");
        prepareFeaturesForTesting("esb1", "servicemix-shared", "jboss-fuse-minimal", "servicemix-shared");
        prepareFeaturesForTesting("esb1", "servicemix-cxf-bc", "jboss-fuse-minimal", "servicemix-cxf-bc");
        prepareFeaturesForTesting("esb1", "servicemix-file", "jboss-fuse-minimal", "servicemix-file");
        prepareFeaturesForTesting("esb1", "servicemix-ftp", "jboss-fuse-minimal", "servicemix-ftp");
        prepareFeaturesForTesting("esb1", "servicemix-http", "jboss-fuse-minimal", "servicemix-http");
        prepareFeaturesForTesting("esb1", "servicemix-jms", "jboss-fuse-minimal", "servicemix-jms");
        prepareFeaturesForTesting("esb1", "servicemix-mail", "jboss-fuse-minimal", "servicemix-mail");
        prepareFeaturesForTesting("esb1", "servicemix-bean", "jboss-fuse-minimal", "servicemix-bean");
        prepareFeaturesForTesting("esb1", "servicemix-camel", "jboss-fuse-minimal", "servicemix-camel");
        prepareFeaturesForTesting("esb1", "servicemix-drools", "jboss-fuse-minimal", "servicemix-drools");
        prepareFeaturesForTesting("esb1", "servicemix-cxf-se", "jboss-fuse-minimal", "servicemix-cxf-se");
        prepareFeaturesForTesting("esb1", "servicemix-eip", "jboss-fuse-minimal", "servicemix-eip");
        prepareFeaturesForTesting("esb1", "servicemix-osworkflow", "jboss-fuse-minimal", "servicemix-osworkflow");
        prepareFeaturesForTesting("esb1", "servicemix-quartz", "jboss-fuse-minimal", "servicemix-quartz");
        prepareFeaturesForTesting("esb1", "servicemix-scripting", "jboss-fuse-minimal", "servicemix-scripting");
        prepareFeaturesForTesting("esb1", "servicemix-validation", "jboss-fuse-minimal", "servicemix-validation");
        prepareFeaturesForTesting("esb1", "servicemix-saxon", "jboss-fuse-minimal", "servicemix-saxon");
        prepareFeaturesForTesting("esb1", "servicemix-wsn2005", "jboss-fuse-minimal", "servicemix-wsn2005");
        prepareFeaturesForTesting("esb1", "servicemix-snmp", "jboss-fuse-minimal", "servicemix-snmp");
        prepareFeaturesForTesting("esb1", "servicemix-vfs", "jboss-fuse-minimal", "servicemix-vfs");
        prepareFeaturesForTesting("esb1", "servicemix-smpp", "jboss-fuse-minimal", "servicemix-smpp");
        prepareFeaturesForTesting("esb1", "activemq-broker", "jboss-fuse-minimal", "activemq-broker");
    }
    
    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("esb1");
    }
}
