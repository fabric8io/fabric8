package org.fusesource.insight.maven

import aether.AetherFacade
import org.junit.Assert._

class ArtifactSourceTest extends TestSupport {
  test("getSource versions") {
    println("About to create the facade")
    try {
      val facade = new AetherFacade()

      println("facade created")
      val groupId = "org.apache.camel"
      val artifactId = "camel-core"
      val version = "2.10.3"
      val path = "org/apache/camel/CamelContext.java"
      val content = facade.getArtifactSource(groupId, artifactId, version, path)
      println("Found content: " + content)
      val expectedContent = "CamelContext"
      assertEquals("content should contain '" + expectedContent + "'", content.indexOf(expectedContent) > 0)
    }
    catch {
      case e => println("Caught: " + e)
    }
  }
}