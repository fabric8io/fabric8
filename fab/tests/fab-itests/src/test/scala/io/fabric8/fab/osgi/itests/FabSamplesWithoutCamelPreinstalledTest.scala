package io.fabric8.fab.osgi.itests

import javax.inject.Inject

import org.junit.Test
import org.junit.Ignore
import org.junit.Assert._
import org.osgi.framework.BundleContext
import org.ops4j.pax.exam.{Configuration, Option}

import org.junit.runner.RunWith
import org.ops4j.pax.exam.junit.PaxExam
;

/**
 * Integration test to ensure a plain FAB can get installed correctly
 */
@RunWith(classOf[PaxExam])
class FabSamplesWithoutCamelPreinstalledTest extends FabIntegrationTestSupport {

  @Inject
  var context: BundleContext = null;

  @Configuration
  def config: Array[Option] = baseConfiguration

  @Test
  def testCamelBlueprintShare = {
    val url = fab("io.fabric8.fab.tests", "fab-sample-camel-blueprint-share")

    val (change, _) = bundlesChanged(context) {
      context.installBundle(url)
    }

    assertTrue("Expected FAB itself to be added", change.map(_.getLocation).contains(url))
    assertTrue("Expected camel-core to be added", change.map(_.getSymbolicName).contains("org.apache.camel.camel-core"))
    assertTrue("Expected camel-blueprint to be added", change.map(_.getSymbolicName).contains("org.apache.camel.camel-core"))
  }
}
