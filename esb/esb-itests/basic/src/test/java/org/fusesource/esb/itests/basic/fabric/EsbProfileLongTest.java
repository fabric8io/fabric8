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
package org.fusesource.esb.itests.basic.fabric;

import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerProxy;

import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class EsbProfileLongTest extends EsbFeatureTest {

    @Test
    public void testFeatures() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            CuratorFramework curator = fabricService.adapt(CuratorFramework.class);

            Set<ContainerProxy> containers = ContainerBuilder.create(fabricProxy).withName("esb").withProfiles("jboss-fuse-minimal").assertProvisioningResult().build();
            try {
                prepareFeaturesForTesting(containers, "connector", "jboss-fuse-minimal", "geronimo-connector");
                prepareFeaturesForTesting(containers, "saaj", "jboss-fuse-minimal", "saaj-impl");
                prepareFeaturesForTesting(containers, "cxf-nmr", "jboss-fuse-minimal", "org.apache.servicemix.cxf.binding.nmr");
                prepareFeaturesForTesting(containers, "camel-nmr", "jboss-fuse-minimal", "org.apache.servicemix.camel.component");

                /*
                 Running all the servicemix-xxxx components leads to Perm-Gen Errors.

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
                 */

                assertFeatures(fabricService, curator);
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }
}
