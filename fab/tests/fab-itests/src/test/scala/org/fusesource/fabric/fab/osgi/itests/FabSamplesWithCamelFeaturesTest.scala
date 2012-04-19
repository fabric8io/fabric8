package org.fusesource.fabric.fab.osgi.itests

import javax.inject.Inject

import org.junit.Test
import org.junit.Assert._
import org.osgi.framework.{Bundle, BundleContext}

import org.ops4j.pax.exam.CoreOptions._;
import org.junit.runner.RunWith
import org.ops4j.pax.exam.junit.{ExamReactorStrategy, JUnit4TestRunner, Configuration}
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory
import org.fusesource.fabric.fab.osgi.internal.Bundles
import org.apache.karaf.features.FeaturesService
import org.ops4j.pax.exam.{MavenUtils, Option}
import java.net.URI
;

/**
 *
 */
@RunWith(classOf[JUnit4TestRunner])
@ExamReactorStrategy(Array(classOf[AllConfinedStagedReactorFactory]))
class FabSamplesWithCamelFeaturesTest {

  lazy val VERSION = System.getProperty("project.version")
  lazy val CAMEL_VERSION = try {
    MavenUtils.getArtifactVersion("org.apache.camel", "camel-core")
  } catch {
    case e: RuntimeException => System.getProperty("camel.version")
  }
  lazy val KARAF_VERSION = try {
    MavenUtils.getArtifactVersion("org.apache.karaf.features", "org.apache.karaf.features.core")
  } catch {
    case e: RuntimeException => System.getProperty("karaf.version")
  }


  @Inject
  var context: BundleContext = null;

  @Inject
  val service: FeaturesService = null;

  @Configuration
  def config: Array[Option] = Array(
    junitBundles(),
    felix(),
    equinox(),

    //vmOption( "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005" ),
    systemProperty("project.version").value(VERSION),
    systemProperty("camel.version").value(CAMEL_VERSION),
    systemProperty("karaf.version").value(KARAF_VERSION),

    // we need the boot delegation to allow the Spring/Blueprint XML parsing with JAXP to succeed
    bootDelegationPackage("com.sun.*"),

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
    mavenBundle("org.apache.karaf.features", "org.apache.karaf.features.core").versionAsInProject(),
    mavenBundle("org.apache.karaf.shell", "org.apache.karaf.shell.console").versionAsInProject(),

    // and then add a few extra bundles to it to enable Scala- and FAB-support
    mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.scala-library").versionAsInProject(),
    mavenBundle("org.fusesource.fabric.fab", "fab-osgi").versionAsInProject()
  )

  /**
   * Get the fab: url for a given example
   *
   * @param groupId the artifact's group id
   * @param artifactId the artifact id
   * @return a fab: url
   */
  def fab(groupId: String, artifactId: String) = "fab:mvn:%s/%s/%s".format(groupId, artifactId, VERSION)

  @Test
  def testCamelBlueprintShare = {
    // configure the feature URLs
    service.addRepository(new URI(String.format("mvn:org.apache.karaf.assemblies.features/standard/%s/xml/features", KARAF_VERSION)))
    service.addRepository(new URI(String.format("mvn:org.apache.camel.karaf/apache-camel/%s/xml/features", CAMEL_VERSION)))

    // let's install the FAB
    val bundle = context.installBundle(fab("org.fusesource.fabric.fab.tests", "fab-sample-camel-velocity-share"))

    // ensure the FAB got installed
    assertNotNull(bundle)
    assertTrue("Bundle should be installed or resolved", bundle.getState >= Bundle.INSTALLED)

    // ensure the required features got installed
    assertTrue("camel-blueprint feature was installed automatically", service.isInstalled(service.getFeature("camel-blueprint")))
    assertTrue("camel-velocity feature was installed automatically", service.isInstalled(service.getFeature("camel-velocity")))
  }

  /**
   * Determines the list of bundles added to the bundle context while executing a block of code
   *
   * @param context the bundle context
   * @param block the block of code to be executed
   * @return a set of bundles that have been added, together with the result of the code block
   */
  def bundlesChanged[R](context: BundleContext)(block: => R): (Set[Bundle], R) = {
    val start = context.getBundles
    val result = block
    (context.getBundles.toSet -- start, result)
  }
}