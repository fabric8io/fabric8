package io.fabric8.fab.osgi.itests

import org.ops4j.pax.exam.{MavenUtils, Option}
import scala.Array
import org.ops4j.pax.exam.CoreOptions._
import org.osgi.framework.{Bundle, BundleContext}

/**
 * The basic Pax Exam configuration for our integration tests
 */
trait FabIntegrationTestSupport {

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
  lazy val LOCAL_REPOSITORY = System.getProperty("org.ops4j.pax.url.mvn.localRepository")
  lazy val REPOSITORIES = Array("http://repo1.maven.org/maven2/",
    "https://repo.fusesource.com/nexus/content/repositories/public",
    "https://repo.fusesource.com/nexus/content/groups/ea",
    "http://repo.fusesource.com/nexus/groups/m2-proxy").mkString(",")

  /**
   * The base Pax Exam configuration
   */
  val baseConfiguration: Array[Option] = Array(
    junitBundles(),

    // vmOption( "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006" ),
    systemProperty("project.version").value(VERSION),
    systemProperty("org.ops4j.pax.url.mvn.localRepository").value(LOCAL_REPOSITORY),
    systemProperty("karaf.etc").value("src/test/resources"),

    // we need the boot delegation to allow the Spring/Blueprint XML parsing with JAXP to succeed
    bootDelegationPackage("com.sun.*"),

    mavenBundle("org.ops4j.pax.logging", "pax-logging-api").versionAsInProject(),
    mavenBundle("org.ops4j.pax.url", "pax-url-mvn").versionAsInProject(),

    mavenBundle("org.apache.felix", "org.apache.felix.fileinstall").versionAsInProject(),
    mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject(),
    mavenBundle("org.apache.felix", "org.apache.felix.gogo.runtime").versionAsInProject(),

    mavenBundle("org.apache.karaf.jaas", "org.apache.karaf.jaas.boot").versionAsInProject(),
    mavenBundle("org.apache.karaf.jaas", "org.apache.karaf.jaas.config").versionAsInProject(),
    mavenBundle("org.apache.karaf.jaas", "org.apache.karaf.jaas.modules").versionAsInProject(),

    mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.asm").versionAsInProject(),
    mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
    mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject(),
    mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.api").versionAsInProject(),
    mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core").versionAsInProject(),
    mavenBundle("org.apache.karaf.features", "org.apache.karaf.features.core").versionAsInProject(),
    mavenBundle("org.apache.mina", "mina-core").versionAsInProject(),
    mavenBundle("org.apache.sshd", "sshd-core").versionAsInProject(),
    mavenBundle("org.apache.karaf.shell", "org.apache.karaf.shell.console").versionAsInProject(),
    mavenBundle("org.apache.karaf.shell", "org.apache.karaf.shell.osgi").versionAsInProject(),

    // and then add a few extra bundles to it to enable Scala- and FAB-support
    mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.scala-library").versionAsInProject(),
    mavenBundle("io.fabric8", "common-util").versionAsInProject(),
    mavenBundle("io.fabric8.fab", "fab-osgi").versionAsInProject()
  )

  /**
   * Get the fab: url for a given example
   *
   * @param groupId the artifact's group id
   * @param artifactId the artifact id
   * @return a fab: url
   */
  def fab(groupId: String, artifactId: String) = "fab:mvn:%s/%s/%s".format(groupId, artifactId, VERSION)

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
