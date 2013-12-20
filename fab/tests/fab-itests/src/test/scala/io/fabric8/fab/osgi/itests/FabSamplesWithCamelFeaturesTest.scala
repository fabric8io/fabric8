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
