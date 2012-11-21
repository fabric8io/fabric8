/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.esb.itests.pax.exam.karaf;

import org.apache.karaf.features.FeaturesService;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class EsbFeaturesTest extends EsbTestSupport {

	@Ignore // JIRA xxxx
    @Test
    public void testConnector() throws Exception {
        installAndCheckFeature("connector");
        unInstallAndCheckFeature("connector");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testSaaj() throws Exception {
        installAndCheckFeature("saaj");
        unInstallAndCheckFeature("saaj");
    }
	
	@Ignore // JIRA xxxx
    @Test
    public void testCxfOsgi() throws Exception {
        installAndCheckFeature("cxf-osgi");
        unInstallAndCheckFeature("cxf-osgi");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testCxfJaxrs() throws Exception {
        FeaturesService s;
        installAndCheckFeature("cxf-jaxrs");
        unInstallAndCheckFeature("cxf-jaxrs");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testCxfNmr() throws Exception {
        installAndCheckFeature("cxf-nmr");
        unInstallAndCheckFeature("cxf-nmr");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testCamelNmr() throws Exception {
        installAndCheckFeature("camel-nmr");
        unInstallAndCheckFeature("camel-nmr");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testCamelActivemq() throws Exception {
        installAndCheckFeature("activmq-camel");
        unInstallAndCheckFeature("activemq-camel");
    }

    @Test
    public void testJpaHibernate() throws Exception {
        installAndCheckFeature("jpa-hibernate");
        unInstallAndCheckFeature("jpa-hibernate");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testServicemixShared() throws Exception {
        installAndCheckFeature("servicemix-shared");
        unInstallAndCheckFeature("servicemix-shared");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testServicemixCxfBc() throws Exception {
        installAndCheckFeature("servicemix-cxf-bc");
        unInstallAndCheckFeature("servicemix-cxf-bc");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testServicemixFile() throws Exception {
        installAndCheckFeature("servicemix-file");
        unInstallAndCheckFeature("servicemix-file");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testServicemixFtp() throws Exception {
        installAndCheckFeature("servicemix-ftp");
        unInstallAndCheckFeature("servicemix-ftp");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testServicemixHttp() throws Exception {
        installAndCheckFeature("servicemix-http");
        unInstallAndCheckFeature("servicemix-http");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testServicemixJms() throws Exception {
        installAndCheckFeature("servicemix-jms");
        unInstallAndCheckFeature("servicemix-jms");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testServicemixMail() throws Exception {
        installAndCheckFeature("servicemix-mail");
        unInstallAndCheckFeature("servicemix-mail");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testServicemixDrools() throws Exception {
        installAndCheckFeature("servicemix-drools");
        unInstallAndCheckFeature("servicemix-drools");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testServicemixCxfSe() throws Exception {
        installAndCheckFeature("servicemix-cxf-se");
        unInstallAndCheckFeature("servicemix-cxf-se");
    }

	@Ignore // JIRA xxxx
    @Test
    public void testServicemixEip() throws Exception {
        installAndCheckFeature("servicemix-eip");
        unInstallAndCheckFeature("servicemix-eip");
    }

    @Ignore // JIRA xxxx
    @Test
    public void testServicemixOsWorkflow() throws Exception {
        installAndCheckFeature("servicemix-osworkflow");
        unInstallAndCheckFeature("servicemix-osworkflow");
    }

    @Ignore // JIRA xxxx
    @Test
    public void testServicemixQuartz() throws Exception {
        installAndCheckFeature("servicemix-quartz");
        unInstallAndCheckFeature("servicemix-quartz");
    }

    
    @Test
    @Ignore("This makes the test hung")
    public void testServicemixScripting() throws Exception {
        installAndCheckFeature("servicemix-scripting");
        unInstallAndCheckFeature("servicemix-scripting");
    }

    @Ignore // JIRA xxxx
    @Test
    public void testServicemixValidation() throws Exception {
        installAndCheckFeature("servicemix-validation");
        unInstallAndCheckFeature("servicemix-validation");
    }

    @Ignore // JIRA xxxx
    @Test
    public void testServicemixSaxon() throws Exception {
        installAndCheckFeature("servicemix-saxon");
        unInstallAndCheckFeature("servicemix-saxon");
    }

    @Test
    @Ignore
    public void testServicemixWsn2005() throws Exception {
        installAndCheckFeature("mq-fabric");
        installAndCheckFeature("servicemix-wsn2005");
        unInstallAndCheckFeature("servicemix-wsn2005");
    }

    @Ignore // JIRA xxxx
    @Test
    public void testServicemixSnmp() throws Exception {
        installAndCheckFeature("servicemix-snmp");
        unInstallAndCheckFeature("servicemix-snmp");
    }

    @Ignore // JIRA xxxx
    @Test
    public void testServicemixVfs() throws Exception {
        installAndCheckFeature("servicemix-vfs");
        unInstallAndCheckFeature("servicemix-vfs");
    }

    @Ignore // JIRA xxxx
    @Test
    public void testServicemixSmpp() throws Exception {
        installAndCheckFeature("servicemix-smpp");
        unInstallAndCheckFeature("servicemix-smpp");
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                esbDistributionConfiguration(), keepRuntimeFolder(),
                editConfigurationFilePut("system.properties", "esb.version", MavenUtils.asInProject().getVersion(GROUP_ID, ARTIFACT_ID)),
                logLevel(LogLevelOption.LogLevel.INFO)};
    }
}
