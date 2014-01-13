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

package org.fusesource.esb.itests.extra;

import org.fusesource.esb.itests.pax.exam.karaf.EsbTestSupport;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class EsbFeaturesTest extends EsbTestSupport {

    @Test
    public void testConnector() throws Exception {
        installUninstallCommand("connector", false);
    }

    @Test
    public void testSaaj() throws Exception {
        installUninstallCommand("saaj");
    }

    @Test
    public void testCxfNmr() throws Exception {
        installUninstallCommand("cxf-nmr");
    }

    @Test
    public void testCamelNmr() throws Exception {
        installUninstallCommand("camel-nmr");
    }

    @Test
    public void testJpaHibernate() throws Exception {
        installUninstallCommand("jpa-hibernate", false);
    }

    @Test
    public void testServicemixShared() throws Exception {
        installUninstallCommand("servicemix-shared", false);
    }

    @Test
    public void testServicemixCxfBc() throws Exception {
        installUninstallCommand("servicemix-cxf-bc", false);
    }

    @Test
    public void testServicemixFile() throws Exception {
        installUninstallCommand("servicemix-file", false);
    }

    @Test
    public void testServicemixFtp() throws Exception {
        installUninstallCommand("servicemix-ftp", false);
    }

    @Test
    public void testServicemixHttp() throws Exception {
        installUninstallCommand("servicemix-http", false);
    }

    @Test
    public void testServicemixJms() throws Exception {
        installUninstallCommand("servicemix-jms", false);
    }

    @Test
    public void testServicemixMail() throws Exception {
        installUninstallCommand("servicemix-mail", false);
    }

    @Test
    public void testServicemixDrools() throws Exception {
        installUninstallCommand("servicemix-drools", false);
    }

    @Test
    public void testServicemixCxfSe() throws Exception {
        installUninstallCommand("servicemix-cxf-se", false);
    }

    @Test
    public void testServicemixEip() throws Exception {
        installUninstallCommand("servicemix-eip", false);
    }

    @Test
    public void testServicemixCamel() throws Exception {
        installUninstallCommand("servicemix-camel", false);
    }

    @Test
    public void testServicemixOsWorkflow() throws Exception {
        installUninstallCommand("servicemix-osworkflow", false);
    }

    @Test
    public void testServicemixQuartz() throws Exception {
        installUninstallCommand("servicemix-quartz", false);
    }

    @Test
    public void testServicemixScripting() throws Exception {
        installUninstallCommand("servicemix-scripting", false);
    }

    @Test
    public void testServicemixValidation() throws Exception {
        installUninstallCommand("servicemix-validation", false);
    }

    @Test
    public void testServicemixSaxon() throws Exception {
        installUninstallCommand("servicemix-saxon");
    }

    @Test
    public void testServicemixWsn2005() throws Exception {
        installUninstallCommand("servicemix-wsn2005", false);
    }

    @Test
    public void testServicemixSnmp() throws Exception {
        installUninstallCommand("servicemix-snmp", false);
    }

    @Test
    public void testServicemixVfs() throws Exception {
        installUninstallCommand("servicemix-vfs", false);
    }

    @Test
    public void testServicemixSmpp() throws Exception {
        installUninstallCommand("servicemix-smpp", false);
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(esbDistributionConfiguration("jboss-fuse-medium")),
        };
    }
}
