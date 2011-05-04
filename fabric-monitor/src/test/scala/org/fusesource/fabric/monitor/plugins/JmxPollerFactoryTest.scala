package org.fusesource.fabric.monitor.plugins

import org.fusesource.fabric.monitor.api.DataSourceDTO

class JmxPollerFactoryTest extends FunSuiteSupport {

  test("monitors some JMX stats") {
    val dto = new DataSourceDTO()
    dto.id = "jmx.threadCount"
    dto.name = "JVM Thread Count"
    dto.description = "JVM Thread Count"
    dto.kind = "guage"
    dto.poll = new MBeanAttributePollDTO("java.lang:type=Threading", "ThreadCount")

    val dtos = Array(dto)
    val poller = new JmxPollerFactory

    expect(true, "should accept: " + dto) {
      poller.accepts(dto)
    }

    // TODO now lets do some polling!

  }
}