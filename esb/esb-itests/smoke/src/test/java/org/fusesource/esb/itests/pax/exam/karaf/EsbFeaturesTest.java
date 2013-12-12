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
    @Ignore("[ENTESB-1049] connector feature does not install")
    public void testConnector() throws Exception {
        installUninstallCommand("connector");
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
    @Ignore("[ENTESB-1035] connector feature does not install")
    public void testJpaHibernate() throws Exception {
        installUninstallCommand("jpa-hibernate");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")
    public void testServicemixShared() throws Exception {
        installUninstallCommand("servicemix-shared");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")   
    public void testServicemixCxfBc() throws Exception {
        installUninstallCommand("servicemix-cxf-bc");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")   
    public void testServicemixFile() throws Exception {
        installUninstallCommand("servicemix-file");        
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")   
    public void testServicemixFtp() throws Exception {
        installUninstallCommand("servicemix-ftp");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")   
    public void testServicemixHttp() throws Exception {
        installUninstallCommand("servicemix-http");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")  
    public void testServicemixJms() throws Exception {
        installUninstallCommand("servicemix-jms");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")   
    public void testServicemixMail() throws Exception {
        installUninstallCommand("servicemix-mail");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")   
    public void testServicemixDrools() throws Exception {
        installUninstallCommand("servicemix-drools");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")
    public void testServicemixCxfSe() throws Exception {
        installUninstallCommand("servicemix-cxf-se");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")  
    public void testServicemixEip() throws Exception {
        installUninstallCommand("servicemix-eip");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")  
    public void testServicemixCamel() throws Exception {
        installUninstallCommand("servicemix-camel");
    }
    
    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")
    public void testServicemixOsWorkflow() throws Exception {
        installUninstallCommand("servicemix-osworkflow");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")
    public void testServicemixQuartz() throws Exception {
        installUninstallCommand("servicemix-quartz");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")
    public void testServicemixScripting() throws Exception {
        installUninstallCommand("servicemix-scripting");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")
    public void testServicemixValidation() throws Exception {
        installUninstallCommand("servicemix-validation");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")
    public void testServicemixSaxon() throws Exception {
        installUninstallCommand("servicemix-saxon");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")
    public void testServicemixWsn2005() throws Exception {        
        installUninstallCommand("servicemix-wsn2005");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")
    public void testServicemixSnmp() throws Exception {
        installUninstallCommand("servicemix-snmp");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")
    public void testServicemixVfs() throws Exception {
        installUninstallCommand("servicemix-vfs");
    }

    @Test
    @Ignore("[ENTESB-1050] JBI features not installing within pax-exam")
    public void testServicemixSmpp() throws Exception {
        installUninstallCommand("servicemix-smpp");
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(esbDistributionConfiguration("jboss-fuse-full")),
        };
    }
}
