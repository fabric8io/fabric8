package org.fusesource.fabric.itests.paxexam.esb;

import org.fusesource.fabric.itests.paxexam.FabricCommandsTestSupport;
import org.fusesource.fabric.itests.paxexam.FabricFeaturesTest;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class EsbProfileLongTest extends FabricFeaturesTest {

    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("esb1");
    }

    @Test
    public void testFeatures() throws Exception {
        System.err.println(executeCommand("fabric:create"));
        createAndAssetChildContainer("esb1", "root", "esb");
        assertProvisionedFeature("esb1", "connector", "esb", "geronimo-connector");
        assertProvisionedFeature("esb1", "saaj", "esb", "saaj-api");
        assertProvisionedFeature("esb1", "cxf-osgi", "esb", "cxf-osgi");
        assertProvisionedFeature("esb1", "cxf-jaxrs", "esb", "cxf-jaxrs");
        assertProvisionedFeature("esb1", "cxf-nmr", "esb", "cxf-nmr");
        assertProvisionedFeature("esb1", "camel-nmr", "esb", "camel-nmr");
        assertProvisionedFeature("esb1", "camel-activemq", "esb", "camel-activemq");
        assertProvisionedFeature("esb1", "examples-cxf-osgi", "esb", "examples-cxf-osgi");
        assertProvisionedFeature("esb1", "examples-cxf-jaxrs", "esb", "examples-cxf-jaxrs");
        assertProvisionedFeature("esb1", "examples-cxf-nmr", "esb", "examples-cxf-nmr");
        assertProvisionedFeature("esb1", "examples-camel-osgi", "esb", "examples-camel-osgi");
        assertProvisionedFeature("esb1", "examples-camel-blueprint", "esb", "examples-camel-blueprint");
        assertProvisionedFeature("esb1", "examples-camel-nmr", "esb", "examples-camel-nmr");
        assertProvisionedFeature("esb1", "examples-camel-nmr-blueprint", "esb", "examples-camel-nmr-blueprint");
        assertProvisionedFeature("esb1", "examples-cxf-camel-nmr", "esb", "examples-cxf-camel-nmr");
        assertProvisionedFeature("esb1", "examples-cxf-ws-addressing", "esb", "examples-cxf-ws-addressing");
        assertProvisionedFeature("esb1", "examples-cxf-wsdl-first-osgi-package", "esb", "examples-cxf-wsdl-first-osgi-package");
        assertProvisionedFeature("esb1", "examples-cxf-ws-security-osgi", "esb", "examples-cxf-ws-security-osgi");
        assertProvisionedFeature("esb1", "jpa-hibernate", "esb", "jpa-hibernate");
        assertProvisionedFeature("esb1", "examples-jpa-osgi", "esb", "examples-jpa-osgi");
        assertProvisionedFeature("esb1", "examples-cxf-ws-rm", "esb", "examples-cxf-ws-rm");
        assertProvisionedFeature("esb1", "servicemix-shared", "esb", "servicemix-shared");
        assertProvisionedFeature("esb1", "servicemix-cxf-bc", "esb", "servicemix-cxf-bc");
        assertProvisionedFeature("esb1", "servicemix-file", "esb", "servicemix-file");
        assertProvisionedFeature("esb1", "servicemix-ftp", "esb", "servicemix-ftp");
        assertProvisionedFeature("esb1", "servicemix-http", "esb", "servicemix-http");
        assertProvisionedFeature("esb1", "servicemix-jms", "esb", "servicemix-jms");
        assertProvisionedFeature("esb1", "servicemix-mail", "esb", "servicemix-mail");
        assertProvisionedFeature("esb1", "servicemix-bean", "esb", "servicemix-bean");
        assertProvisionedFeature("esb1", "servicemix-camel", "esb", "servicemix-camel");
        assertProvisionedFeature("esb1", "servicemix-drools", "esb", "servicemix-drools");
        assertProvisionedFeature("esb1", "servicemix-cxf-se", "esb", "servicemix-cxf-se");
        assertProvisionedFeature("esb1", "servicemix-eip", "esb", "servicemix-eip");
        assertProvisionedFeature("esb1", "servicemix-osworkflow", "esb", "servicemix-osworkflow");
        assertProvisionedFeature("esb1", "servicemix-quartz", "esb", "servicemix-quartz");
        assertProvisionedFeature("esb1", "servicemix-scripting", "esb", "servicemix-scripting");
        assertProvisionedFeature("esb1", "servicemix-validation", "esb", "servicemix-validation");
        assertProvisionedFeature("esb1", "servicemix-saxon", "esb", "servicemix-saxon");
        assertProvisionedFeature("esb1", "servicemix-wsn2005", "esb", "servicemix-wsn2005");
        assertProvisionedFeature("esb1", "servicemix-snmp", "esb", "servicemix-snmp");
        assertProvisionedFeature("esb1", "servicemix-vfs", "esb", "servicemix-vfs");
        assertProvisionedFeature("esb1", "servicemix-smpp", "esb", "servicemix-smpp");
        assertProvisionedFeature("esb1", "servicemix-exec", "esb", "servicemix-exec");
        assertProvisionedFeature("esb1", "activemq-broker", "esb", "activemq-broker");
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                fabricDistributionConfiguration(), keepRuntimeFolder(),
                logLevel(LogLevelOption.LogLevel.ERROR)};
    }
}
