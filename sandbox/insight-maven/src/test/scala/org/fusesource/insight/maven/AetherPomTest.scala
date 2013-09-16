package org.fusesource.insight.maven

import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import java.io.FileInputStream
import scala.collection.JavaConversions._

class AetherPomTest extends TestSupport {
  test("aether") {
    val result = aether.resolvePom("org.apache.camel", "camel", "2.9.0.fuse-70-084")
    println("got " + result)
  }
}