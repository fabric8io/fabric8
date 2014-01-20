package io.fabric8.insight.maven

import io.fabric8.insight.maven.aether.Aether

class AetherTest extends TestSupport {
  test("aether") {
    aether.resolve("org.apache.camel", "camel-core", "2.5.0")
  }
}