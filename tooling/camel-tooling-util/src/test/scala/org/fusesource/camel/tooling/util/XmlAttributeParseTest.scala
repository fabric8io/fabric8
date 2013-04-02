package org.fusesource.camel.tooling.util

import java.io.File
import org.junit.Assert._

/**
 */
class XmlAttributeParseTest extends RouteXmlTestSupport {

  test("parses valid XML file with > in xml attribute") {
    val x = assertRoutes(new File(baseDir, "src/test/resources/blueprintWithGreaterThanInAttribute.xml"), 1, CamelNamespaces.blueprintNS)

    val uris = x.endpointUris
    expect(1, "endpoint uris " + uris){ uris.size }
    assertTrue(uris.contains("seda:myConfiguredEndpoint"))
  }
}