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
package io.fabric8.fab.osgi.itests

import javax.inject.Inject

import org.junit.Test
import org.junit.Ignore
import org.junit.Assert._
import org.osgi.framework.{Bundle, BundleContext}

import org.junit.runner.RunWith
import org.ops4j.pax.exam.junit.PaxExam
import org.apache.karaf.features.FeaturesService
import org.ops4j.pax.exam.{Configuration, Option}
import java.net.URI
;

/**
 * Integration test to ensure Camel features can get installed auto-magically
 */
@RunWith(classOf[PaxExam])
class FabSamplesWithCamelFeaturesTest extends FabIntegrationTestSupport{

  @Inject
  var context: BundleContext = null;

  @Inject
  val service: FeaturesService = null;

  @Configuration
  def config: Array[Option] = baseConfiguration

  @Test
  def testCamelBlueprintShare = {
    // configure the feature URLs
    service.addRepository(new URI(String.format("mvn:org.apache.karaf.assemblies.features/standard/%s/xml/features", KARAF_VERSION)))
    service.addRepository(new URI(String.format("mvn:org.apache.camel.karaf/apache-camel/%s/xml/features", CAMEL_VERSION)))

    // let's install the FAB
    val bundle = context.installBundle(fab("io.fabric8.fab.tests", "fab-sample-camel-velocity-share"))

    // ensure the FAB got installed
    assertNotNull(bundle)
    assertTrue("Bundle should be installed or resolved", bundle.getState >= Bundle.INSTALLED)

    // ensure the required features got installed
    assertTrue("camel-blueprint feature was installed automatically", service.isInstalled(service.getFeature("camel-blueprint")))
    assertTrue("camel-velocity feature was installed automatically", service.isInstalled(service.getFeature("camel-velocity")))
  }
}
