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

import java.net.URI;

import javax.inject.Inject;

import org.apache.karaf.features.FeaturesService;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FabSamplesWithCamelFeaturesTest extends FabIntegrationTestSupport {

    @Inject
    private BundleContext context;

    @Inject
    private FeaturesService service;

    @Test
    public void testCamelBlueprintShare() throws Exception {
        await(context, "(&(objectClass=org.osgi.service.url.URLStreamHandlerService)(url.handler.protocol=fab))");

        // configure the feature URLs
        service.addRepository(new URI(String.format("mvn:org.apache.karaf.assemblies.features/standard/%s/xml/features", KARAF_VERSION)));
        service.addRepository(new URI(String.format("mvn:org.apache.camel.karaf/apache-camel/%s/xml/features", CAMEL_VERSION)));

        // let's install the FAB
        String url = fab("io.fabric8.fab.tests", "fab-sample-camel-velocity-share");
        Bundle bundle = context.installBundle(url);

        // ensure the FAB got installed
        assertNotNull(bundle);
        assertTrue("Bundle should be installed or resolved", bundle.getState() >= Bundle.INSTALLED);

        // ensure the required features got installed
        assertTrue("camel-blueprint feature was installed automatically", service.isInstalled(service.getFeature("camel-blueprint")));
        assertTrue("camel-velocity feature was installed automatically", service.isInstalled(service.getFeature("camel-velocity")));
    }

}
