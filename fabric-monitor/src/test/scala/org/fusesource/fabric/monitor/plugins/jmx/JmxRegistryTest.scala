package org.fusesource.fabric.monitor.plugins
package jmx

import org.fusesource.fabric.monitor.api.DataSourceDTO

import collection.JavaConversions._

class JmxRegistryTest extends FunSuiteSupport {


  test("query JMX beans") {
    val registry = new JmxDataSourceRegistry()

    val answer = registry.findSources
    for (a <- answer) {
      println("Found: " + a)
    }
  }
}