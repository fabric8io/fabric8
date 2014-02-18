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

package org.fusesource.esb.itests.smoke;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import io.fabric8.api.ServiceLocator;

import org.fusesource.esb.itests.pax.exam.karaf.EsbTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
/**
 * A set of smoke tests to ensure that some of our core ESB features are getting installed/started properly
 */
public class EsbCoreFeaturesTest extends EsbTestSupport {

    @Inject
    BundleContext bundleContext;

    @Test
    public void testHawtIo() throws Exception {
        // let's start by checking if the feature got installed correctly
        String listFeaturesOutput = executeCommand("features:list | grep -i \" hawtio \"");
        assertTrue("Feature hawtio is installed", listFeaturesOutput.contains("installed"));

        // ensure that a servlet context has been registered for the /hawtio URL path
        ServiceLocator.awaitService(bundleContext, ServletContext.class, "(osgi.web.contextpath=/hawtio)");
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(esbDistributionConfiguration("jboss-fuse-medium")),
        };
    }

}
