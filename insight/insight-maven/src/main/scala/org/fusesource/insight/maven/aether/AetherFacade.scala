package org.fusesource.insight.maven.aether

/**
 * Represents an MBean for working with Aether
 */
trait AetherFacadeMXBean {

  //  Use extension of "jar" and classifier of "" by default

  def resolve(groupId: String, artifactId: String, version: String,
              extension: String, classifier: String): AetherResult
  /**
   * Compare 2 versions
   */
  def compare(groupId: String, artifactId: String, version1: String, version2: String, extension: String, classifier: String): CompareResult
}

class AetherFacade(val aether: Aether = new Aether()) {


  def resolve(groupId: String, artifactId: String, version: String,
              extension: String, classifier: String): AetherResult
  = aether.resolve(groupId, artifactId, version, extension, classifier)

  def compare(groupId: String, artifactId: String, version1: String, version2: String, extension: String, classifier: String): CompareResult
    =aether.compare(groupId, artifactId, version1, version2, extension, classifier)


}
