package org.fusesource.insight.maven

import aether.Aether

class AetherTest extends TestSupport {
  test("aether") {
    aether.resolve("org.apache.camel", "camel-core", "2.5.0")
  }
}