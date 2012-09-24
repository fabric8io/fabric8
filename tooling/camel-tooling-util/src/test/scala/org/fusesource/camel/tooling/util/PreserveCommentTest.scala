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
import org.apache.camel.model.RouteDefinition
import org.junit.Assert._

class PreserveCommentTest extends RouteXmlTestSupport {

  test("comments before route preserved") {
    val x = assertRoundTrip("src/test/resources/commentBeforeRoute.xml", 2)

    val route1 = x.routeDefinitions(1)
    assertEquals("route4", route1.getId)
    val desc = route1.getDescription
    assertTrue(desc != null)
    assertEquals("route4 description\ncomment about route4", desc.getText)
  }

  test("comments in cameContext preserved") {
    val x = assertRoundTrip("src/test/resources/commentInCamelContext.xml")

    val helper = new RouteXml()
    val newText = helper.marshalToText(x)

    println("newText: " + newText)

    assert(newText.contains("comment in camelContext"))
  }

  test("comments in route preserved") {
    val x = assertRoundTrip("src/test/resources/commentInRoute.xml")

    val route1 = x.routeDefinitions(0)
    val desc = route1.getDescription
    assertTrue(desc != null)
    assertEquals("route3 comment", desc.getText)
  }

  test("comments in route with description preserved") {
    val x = assertRoundTrip("src/test/resources/commentInRouteWithDescription.xml")

    val route1 = x.routeDefinitions(0)
    val desc = route1.getDescription
    assertTrue(desc != null)
    assertEquals("previous description\nnew comment added to previous one", desc.getText)
  }


  def assertRoundTrip(name: String, count: Int = 1): XmlModel = {
    val file = new File(baseDir, name)
    val x = assertRoutes(file, count)

    // now lets modify the xml
    val definitionList = x.getRouteDefinitionList
    val route = new RouteDefinition().from("file:foo").to("file:bar")
    definitionList.add(route)

    println("Round tripped to: " + x)
    x
  }
}