package org.fusesource.fabric.fab.osgi.itests

import javax.inject.Inject

import org.junit.Test
import org.junit.Assert._
import org.osgi.framework.{Bundle, BundleContext}
import org.ops4j.pax.exam.{Configuration, Option}

import org.ops4j.pax.exam.CoreOptions._;
import org.junit.runner.RunWith
import org.ops4j.pax.exam.junit.PaxExam
import org.fusesource.fabric.fab.osgi.internal.Bundles
;

/**
 * Integration test to ensure pre-installed Camel bundles are used
 */
@RunWith (classOf[PaxExam] )
class FabSamplesWithCamelPreinstalledTest extends FabIntegrationTestSupport {

  @Inject
  var context: BundleContext = null;

  @Configuration
  def config : Array[Option] = baseConfiguration ++ Array(
    mavenBundle("org.apache.camel", "camel-core").versionAsInProject(),
    mavenBundle("org.apache.camel", "camel-blueprint").versionAsInProject(),
    mavenBundle("org.apache.camel", "camel-spring").versionAsInProject()
  )

  @Test
  def testCamelBlueprintShare = {
    val url = fab("org.fusesource.fabric.fab.tests", "fab-sample-camel-blueprint-share")

    val (change, _) = bundlesChanged(context) {
      context.installBundle(url)
    }

    assertEquals("Expected only the FAB itself to be added", 1, change.size)
    assertEquals("Expected only the FAB itself to be added", Set(url), change.map(_.getLocation))
  }

  @Test
  def testCamelSpringShare = {
    val url = fab("org.fusesource.fabric.fab.tests", "fab-sample-camel-spring-share")

    val (change, _) = bundlesChanged(context) {
      context.installBundle(url)
    }

    assertEquals("Expected only the FAB itself to be added", 1, change.size)
    assertEquals("Expected only the FAB itself to be added", Set(url), change.map(_.getLocation))
  }

  @Test
  def testCamelNoShare = {
    val url = fab("org.fusesource.fabric.fab.tests", "fab-sample-camel-noshare")

    val (change, result : Bundle) = bundlesChanged(context) {
      context.installBundle(url)
    }

    assertEquals("Expected only the FAB itself to be added", 1, change.size)
    assertEquals("Expected only the FAB itself to be added", Set(url), change.map(_.getLocation))

    val camel = Bundles.findOneBundle(context, "org.apache.camel.camel-core")

    assertNotSame("Installed FAB should not be using the shared camel bundle's classes",
                  result.loadClass("org.apache.camel.CamelContext"), camel.loadClass("org.apache.camel.CamelContext"))
  }

  @Test
  def   testCamelVelocityNoShare = {
    val url = fab("org.fusesource.fabric.fab.tests", "fab-sample-camel-velocity-noshare")

    val (change, result : Bundle) = bundlesChanged(context) {
      context.installBundle(url)
    }

    assertEquals("Expected only the FAB itself to be added", 1, change.size)
    assertEquals("Expected only the FAB itself to be added", Set(url), change.map(_.getLocation))

    val camel = Bundles.findOneBundle(context, "org.apache.camel.camel-core")

    assertNotSame("Installed FAB should not be using the shared camel bundle's classes",
      result.loadClass("org.apache.camel.CamelContext"), camel.loadClass("org.apache.camel.CamelContext"))
  }
}
