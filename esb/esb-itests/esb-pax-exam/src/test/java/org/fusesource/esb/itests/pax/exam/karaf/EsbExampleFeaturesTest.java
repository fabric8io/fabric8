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
public class EsbExampleFeaturesTest extends EsbTestSupport {

    @Test
    public void testCxfOsgi() throws Exception {
        installAndCheckFeature("examples-cxf-osgi");
        unInstallAndCheckFeature("examples-cxf-osgi");
    }

    @Test
    public void testCxfJaxrs() throws Exception {
        FeaturesService s;
        installAndCheckFeature("examples-cxf-jaxrs");
        unInstallAndCheckFeature("examples-cxf-jaxrs");
    }

    @Test
    public void testCxfNmr() throws Exception {
        installAndCheckFeature("examples-cxf-nmr");
        unInstallAndCheckFeature("examples-cxf-nmr");
    }

    @Test
    public void testCamelOsgi() throws Exception {
        installAndCheckFeature("examples-camel-osgi");
        unInstallAndCheckFeature("examples-camel-osgi");
    }


    @Test
    public void testCamelBleuprint() throws Exception {
        installAndCheckFeature("examples-camel-blueprint");
        unInstallAndCheckFeature("examples-camel-blueprint");
    }

    @Test
    public void testCamelNmrBleuprint() throws Exception {
        installAndCheckFeature("examples-camel-nmr-blueprint");
        unInstallAndCheckFeature("examples-camel-nmr-blueprint");
    }

    @Test
    public void testCamelNmr() throws Exception {
        installAndCheckFeature("examples-camel-nmr");
        unInstallAndCheckFeature("examples-camel-nmr");
    }

    @Test
    public void testCxfCamelNmr() throws Exception {
        installAndCheckFeature("examples-cxf-camel-nmr");
        unInstallAndCheckFeature("examples-cxf-camel-nmr");
    }

    @Test
    public void testCxfWsAddressing() throws Exception {
        installAndCheckFeature("examples-cxf-ws-addressing");
        unInstallAndCheckFeature("examples-cxf-ws-addressing");
    }

    @Test
    public void testCxfWsdlFirstOsgiPackage() throws Exception {
        installAndCheckFeature("examples-cxf-wsdl-first-osgi-package");
        unInstallAndCheckFeature("examples-cxf-wsdl-first-osgi-package");
    }

    @Test
    public void testCxfWsSecurityOsgi() throws Exception {
        installAndCheckFeature("examples-cxf-ws-security-osgi");
        unInstallAndCheckFeature("examples-cxf-ws-security-osgi");
    }

    @Test
    public void testCxfWsSecurityBlueprint() throws Exception {
        installAndCheckFeature("examples-cxf-ws-security-blueprint");
        unInstallAndCheckFeature("examples-cxf-ws-security-blueprint");
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                esbDistributionConfiguration(), keepRuntimeFolder(),
                editConfigurationFilePut("system.properties", "esb.version", MavenUtils.asInProject().getVersion(GROUP_ID, ARTIFACT_ID)),
                logLevel(LogLevelOption.LogLevel.INFO)};
    }
}
