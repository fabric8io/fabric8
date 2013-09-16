package org.fusesource.insight.maven.aether

import javax.management.{MBeanServer, ObjectName}
import management.ManagementFactory

/**
 * Represents an MBean for working with Aether
 */
trait AetherFacadeMXBean {

  //  Use extension of "jar" and classifier of "" by default

/*
  def resolve(groupId: String, artifactId: String, version: String,
              extension: String, classifier: String): AetherResult

  /**
   * Compare 2 versions
   */
  def compare(groupId: String, artifactId: String, version1: String, version2: String, extension: String, classifier: String): CompareResult
*/
}

class AetherFacade() extends AetherFacadeMXBean {
  var aether: Aether = new Aether()
  var objectName: ObjectName = null
  var mBeanServer: MBeanServer = null

  def init() {
    if (objectName == null) {
      objectName = new ObjectName("org.fusesource.insight:type=Maven")
    }
    if (mBeanServer == null) {
      mBeanServer = ManagementFactory.getPlatformMBeanServer()
    }
    mBeanServer.registerMBean(this, objectName)
  }

  def destroy() {
    if (objectName != null && mBeanServer != null) {
      mBeanServer.unregisterMBean(objectName)
    }
  }

  def resolve(groupId: String, artifactId: String, version: String,
              extension: String, classifier: String): AetherResult
  = aether.resolve(groupId, artifactId, version, extension, classifier)

  def compare(groupId: String, artifactId: String, version1: String, version2: String, extension: String, classifier: String): CompareResult
  = aether.compare(groupId, artifactId, version1, version2, extension, classifier)
}
