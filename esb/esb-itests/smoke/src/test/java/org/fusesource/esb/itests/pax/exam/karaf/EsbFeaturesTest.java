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
import org.apache.karaf.tooling.exam.options.LogLevelOption;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.keepRuntimeFolder;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
@Ignore("[FABRIC-662] Fix esb smoke EsbFeaturesTest")
public class EsbFeaturesTest extends EsbTestSupport {

    @Test
    public void testConnector() throws Exception {
        installAndCheckFeature("connector");
        unInstallAndCheckFeature("connector");
    }

    @Test
    public void testSaaj() throws Exception {
        installAndCheckFeature("saaj");
        unInstallAndCheckFeature("saaj");
    }

    @Test
    public void testCxfOsgi() throws Exception {
        installAndCheckFeature("cxf-osgi");
        unInstallAndCheckFeature("cxf-osgi");
    }

    @Test
    public void testCxfJaxrs() throws Exception {
        FeaturesService s;
        installAndCheckFeature("cxf-jaxrs");
        unInstallAndCheckFeature("cxf-jaxrs");
    }

    @Test
    public void testCxfNmr() throws Exception {
        installAndCheckFeature("cxf-nmr");
        unInstallAndCheckFeature("cxf-nmr");
    }

    @Test
    public void testCamelNmr() throws Exception {
        installAndCheckFeature("camel-nmr");
        unInstallAndCheckFeature("camel-nmr");
    }

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

    @Test
    public void testServicemixShared() throws Exception {
        installAndCheckFeature("servicemix-shared");
        unInstallAndCheckFeature("servicemix-shared");
    }

    @Test
    public void testServicemixCxfBc() throws Exception {
        installAndCheckFeature("servicemix-cxf-bc");
        unInstallAndCheckFeature("servicemix-cxf-bc");
    }

    @Test
    public void testServicemixFile() throws Exception {
        installAndCheckFeature("servicemix-file");
        unInstallAndCheckFeature("servicemix-file");
    }

    @Test
    public void testServicemixFtp() throws Exception {
        installAndCheckFeature("servicemix-ftp");
        unInstallAndCheckFeature("servicemix-ftp");
    }

    @Test
    public void testServicemixHttp() throws Exception {
        installAndCheckFeature("servicemix-http");
        unInstallAndCheckFeature("servicemix-http");
    }

    @Test
    public void testServicemixJms() throws Exception {
        installAndCheckFeature("servicemix-jms");
        unInstallAndCheckFeature("servicemix-jms");
    }

    @Test
    public void testServicemixMail() throws Exception {
        installAndCheckFeature("servicemix-mail");
        unInstallAndCheckFeature("servicemix-mail");
    }

    @Test
    public void testServicemixDrools() throws Exception {
        installAndCheckFeature("servicemix-drools");
        unInstallAndCheckFeature("servicemix-drools");
    }

    @Test
    public void testServicemixCxfSe() throws Exception {
        installAndCheckFeature("servicemix-cxf-se");
        unInstallAndCheckFeature("servicemix-cxf-se");
    }

    @Test
    public void testServicemixEip() throws Exception {
        installAndCheckFeature("servicemix-eip");
        unInstallAndCheckFeature("servicemix-eip");
    }

    @Test
    public void testServicemixOsWorkflow() throws Exception {
        installAndCheckFeature("servicemix-osworkflow");
        unInstallAndCheckFeature("servicemix-osworkflow");
    }

    @Test
    public void testServicemixQuartz() throws Exception {
        installAndCheckFeature("servicemix-quartz");
        unInstallAndCheckFeature("servicemix-quartz");
    }

    @Test
    public void testServicemixScripting() throws Exception {
        installAndCheckFeature("servicemix-scripting");
        unInstallAndCheckFeature("servicemix-scripting");
    }

    @Test
    public void testServicemixValidation() throws Exception {
        installAndCheckFeature("servicemix-validation");
        unInstallAndCheckFeature("servicemix-validation");
    }

    @Test
    public void testServicemixSaxon() throws Exception {
        installAndCheckFeature("servicemix-saxon");
        unInstallAndCheckFeature("servicemix-saxon");
    }

    @Test
    public void testServicemixWsn2005() throws Exception {
        installAndCheckFeature("mq-fabric");
        installAndCheckFeature("servicemix-wsn2005");
        unInstallAndCheckFeature("servicemix-wsn2005");
    }

    @Test
    public void testServicemixSnmp() throws Exception {
        installAndCheckFeature("servicemix-snmp");
        unInstallAndCheckFeature("servicemix-snmp");
    }

    @Test
    public void testServicemixVfs() throws Exception {
        installAndCheckFeature("servicemix-vfs");
        unInstallAndCheckFeature("servicemix-vfs");
    }

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
