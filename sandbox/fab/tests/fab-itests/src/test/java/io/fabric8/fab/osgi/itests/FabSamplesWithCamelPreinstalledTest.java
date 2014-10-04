/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.fab.osgi.itests;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

import io.fabric8.fab.osgi.internal.Bundles;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

public class FabSamplesWithCamelPreinstalledTest extends FabIntegrationTestSupport {

    @Inject
    private BundleContext context;

    @Test
    public void testCamelBlueprintShare() throws Exception {
        await(context, "(&(objectClass=org.osgi.service.url.URLStreamHandlerService)(url.handler.protocol=fab))");

        Set<Bundle> stateBefore = new HashSet<Bundle>(Arrays.asList(context.getBundles()));

        String url = fab("io.fabric8.fab.tests", "fab-sample-camel-blueprint-share");
        context.installBundle(url);

        Set<Bundle> stateAfter = new HashSet<Bundle>(Arrays.asList(context.getBundles()));

        stateAfter.removeAll(stateBefore);

        assertTrue("Expected only the FAB itself to be added", stateAfter.size() == 1);
        assertTrue("Expected only the FAB itself to be added", stateAfter.iterator().next().getLocation().equals(url));
    }

    @Test
    public void testCamelNoShare() throws Exception {
        await(context, "(&(objectClass=org.osgi.service.url.URLStreamHandlerService)(url.handler.protocol=fab))");

        Set<Bundle> stateBefore = new HashSet<Bundle>(Arrays.asList(context.getBundles()));

        String url = fab("io.fabric8.fab.tests", "fab-sample-camel-noshare");
        context.installBundle(url);

        Set<Bundle> stateAfter = new HashSet<Bundle>(Arrays.asList(context.getBundles()));

        stateAfter.removeAll(stateBefore);

        assertTrue("Expected FAB itself to be added", stateAfter.size() == 1);
        Bundle fabBundle = stateAfter.iterator().next();
        assertTrue("Expected camel-core to be added", fabBundle.getLocation().equals(url));

        Bundle camel = Bundles.findOneBundle(context, "org.apache.camel.camel-core");
        assertNotSame("Installed FAB should not be using the shared camel bundle's classes",
                  fabBundle.loadClass("org.apache.camel.CamelContext"), camel.loadClass("org.apache.camel.CamelContext"));
    }

    @Test
    public void testCamelVelocityNoShare() throws Exception {
        await(context, "(&(objectClass=org.osgi.service.url.URLStreamHandlerService)(url.handler.protocol=fab))");

        Set<Bundle> stateBefore = new HashSet<Bundle>(Arrays.asList(context.getBundles()));

        String url = fab("io.fabric8.fab.tests", "fab-sample-camel-velocity-noshare");
        context.installBundle(url);

        Set<Bundle> stateAfter = new HashSet<Bundle>(Arrays.asList(context.getBundles()));

        stateAfter.removeAll(stateBefore);

        assertTrue("Expected FAB itself to be added", stateAfter.size() == 1);
        Bundle fabBundle = stateAfter.iterator().next();
        assertTrue("Expected camel-core to be added", fabBundle.getLocation().equals(url));

        Bundle camel = Bundles.findOneBundle(context, "org.apache.camel.camel-core");
        assertNotSame("Installed FAB should not be using the shared camel bundle's classes",
                  fabBundle.loadClass("org.apache.camel.CamelContext"), camel.loadClass("org.apache.camel.CamelContext"));
    }

    @Configuration
    public Option[] config() {
        return combine(super.config(),
            mavenBundle("org.apache.camel", "camel-core").versionAsInProject(),
            mavenBundle("org.apache.camel", "camel-blueprint").versionAsInProject()
        );
    }

}
