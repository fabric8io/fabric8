package org.fusesource.insight.maven

import aether.CompareDependencyNode

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

    val c = aether.compare("org.apache.camel", "camel-core", "2.4.0", "2.5.0")
    dump(c.root)
  }
}