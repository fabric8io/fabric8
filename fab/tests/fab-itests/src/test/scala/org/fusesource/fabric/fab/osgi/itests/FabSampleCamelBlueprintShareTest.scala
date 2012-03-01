package org.fusesource.fabric.fab.osgi.itests

import javax.inject.Inject

import org.junit.Test
import org.junit.Assert._
import org.osgi.framework.{Bundle, BundleContext}
import org.ops4j.pax.exam.Option

import org.ops4j.pax.exam.CoreOptions._;
import org.junit.runner.RunWith
import org.ops4j.pax.exam.junit.{ExamReactorStrategy, JUnit4TestRunner, Configuration}
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory
import org.osgi.util.tracker.ServiceTracker
;

/**
 *
 */
@RunWith (classOf[JUnit4TestRunner] )
@ExamReactorStrategy (Array (classOf[AllConfinedStagedReactorFactory] ) )
class FabSampleCamelBlueprintShareTest {

  @Inject
  var context: BundleContext = null;

  @Configuration
  def config : Array[Option] = Array(
    junitBundles(),
    felix(),
    equinox(),

    mavenBundle("org.ops4j.pax.url", "pax-url-mvn").versionAsInProject(),

    mavenBundle("org.apache.felix", "org.apache.felix.fileinstall").versionAsInProject(),
    mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject(),

    mavenBundle("org.apache.karaf.jaas", "org.apache.karaf.jaas.boot").versionAsInProject(),
    mavenBundle("org.apache.karaf.jaas", "org.apache.karaf.jaas.config").versionAsInProject(),
    mavenBundle("org.apache.karaf.jaas", "org.apache.karaf.jaas.modules").versionAsInProject(),

    mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.asm").versionAsInProject(),
    mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
    mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject(),
    mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.api").versionAsInProject(),
    mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core").versionAsInProject(),
    mavenBundle("org.apache.karaf.shell", "org.apache.karaf.shell.console").versionAsInProject(),

    mavenBundle("org.apache.camel", "camel-core").versionAsInProject(),
    mavenBundle("org.apache.camel", "camel-blueprint").versionAsInProject(),

    // and then add a few extra bundles to it to enable Scala- and FAB-support
    mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.scala-library").versionAsInProject(),
    mavenBundle("org.fusesource.fabric.fab", "fab-osgi").versionAsInProject()
  )

  @Test
  def test = {
    val url: String = "fab:mvn:org.fusesource.fabric.fab.tests/fab-sample-camel-blueprint-share/7.0-SNAPSHOT"

    val change = bundlesChanged(context) {
      context.installBundle(url)
    }

    assertEquals("Expected only the FAB itself to be added", 1, change.size)
    assertEquals("Expected only the FAB itself to be added", url, change.head.getLocation)
  }

  /**
   * Determines the list of bundles added to the bundle context while executing a block of code
   *
   * @param context the bundle context
   * @param block the block of code to be executed
   * @return a set of bundles that have been added
   */
  def bundlesChanged(context: BundleContext)(block: => Unit) : Set[Bundle] = {
    val start = context.getBundles
    block
    context.getBundles.toSet -- start
  }
}
