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
package io.fabric8.insight.maven

import aether.{Authentications, CompareDependencyNode}

class CompareTest  extends TestSupport {
  test("compare versions") {
    def dump(node: CompareDependencyNode, indentLevel: Int = 0): Unit = {
      for (i <- 0.to(indentLevel)) {
        print("  ")
      }
      println("" + node.groupId + ":" + node.artifactId + " " + node.change.summary)
      val nextIndent = indentLevel + 1
      for (c <- node.children) {
        dump(c, nextIndent)
      }
    }

    if (Authentications.repoFile.exists()) {
      val c = aether.compare("org.apache.camel", "camel-core", "2.4.0", "2.5.0")
      dump(c.root)
    } else {
      println("Warning: no authentications file at " + Authentications.repoFile)
    }
  }
}