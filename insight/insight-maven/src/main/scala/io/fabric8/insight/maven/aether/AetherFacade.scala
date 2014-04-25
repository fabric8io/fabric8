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
package io.fabric8.insight.maven.aether

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
