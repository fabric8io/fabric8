/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.camel.tooling
package util

import java.io.File
import org.junit.Assert._
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.builder.RouteBuilder

class RouteContextTest extends RouteXmlTestSupport {

  test("parses routeContext spring XML file") {
    val x = assertRoutes(new File(baseDir, "src/test/resources/routeContext.xml"), 1)
  }

  test("parses routeContext blueprint XML file") {
    val x = assertRoutes(new File(baseDir, "src/test/resources/routeContextBP.xml"), 1, CamelNamespaces.blueprintNS)
  }

  test("save routeContext") {
    val x: XmlModel = assertLoadModel(new File(baseDir, "src/test/resources/routeContext.xml"), 1)
    val rc = x.getRouteDefinitionList.size()

    // now lets add a route and write it back again...
    val tmpContext = new DefaultCamelContext
    tmpContext.addRoutes(new RouteBuilder {
      def configure {
        from("seda:newFrom").to("seda:newTo")
      }
    })
    x.contextElement.getRoutes.addAll(tmpContext.getRouteDefinitions)

    val xmlText = tool.marshalToText(x)

    val y: XmlModel = tool.unmarshal(xmlText)

    assertTrue(y.getRouteDefinitionList.size() == x.getRouteDefinitionList.size())
    assertFalse(y.getRouteDefinitionList.size() == rc)
  }

//  test("save routeContext BP") {
//    val x = assertLoadModel(new File(baseDir, "src/test/resources/routeContextBP.xml"), 1)
//    System.err.println(tool.marshalToText(x))
//  }
}