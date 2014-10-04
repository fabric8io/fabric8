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

import javax.inject.Inject;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertTrue;

public class FabSamplesWithoutCamelPreinstalledTest extends FabIntegrationTestSupport {

    @Inject
    private BundleContext context;

    @Test
    public void testCamelBlueprintShare() throws Exception {
        await(context, "(&(objectClass=org.osgi.service.url.URLStreamHandlerService)(url.handler.protocol=fab))");
//        await(context, "(osgi.blueprint.container.symbolicname=org.apache.camel.camel-blueprint)");

        String url = fab("io.fabric8.fab.tests", "fab-sample-camel-blueprint-share");
        context.installBundle(url);

        boolean foundCamelBlueprintShare = false;
        boolean foundCamelCore = false;
        boolean foundCamelBlueprint = false;
        for (Bundle b : context.getBundles()) {
            System.out.println(">>>> " + b.getSymbolicName());
            if (url.equals(b.getLocation())) {
                foundCamelBlueprintShare = true;
            }
            if ("org.apache.camel.camel-core".equals(b.getSymbolicName())) {
                foundCamelCore = true;
            }
            if ("org.apache.camel.camel-blueprint".equals(b.getSymbolicName())) {
                foundCamelBlueprint = true;
            }
        }

        assertTrue("Expected FAB itself to be added", foundCamelBlueprintShare);
        assertTrue("Expected camel-core to be added", foundCamelCore);
        // previously this was checking camel-core instead of camel-blueprint, but
        // for camel-blueprint we have this in logs:
        // Bundle non-optional packages already installed for: org.apache.camel.camel-blueprint version: 2.13.0 packages: []
//        assertTrue("Expected camel-blueprint to be added", foundCamelBlueprint);
    }

}
