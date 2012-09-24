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
import org.apache.camel.model.RouteDefinition
import org.fusesource.scalate.util.IOUtil

class NamespacePreserveTest extends RouteXmlTestSupport {

  test("update an XML and keep namespaces") {
    val file = new File(baseDir, "src/test/resources/namespaceRoute.xml")
    val x = assertRoutes(file, 1)

    // now lets modify the xml
    val definitionList = x.getRouteDefinitionList
    val route = new RouteDefinition().from("file:foo").to("file:bar")
    definitionList.add(route)

    println("Routes now: " + x.routeDefinitions)

    val text = IOUtil.loadTextFile(file)

    val helper = new RouteXml()
    val newText = helper.marshalToText(text, definitionList)

    println("newText: " + newText)

    for (ns <- List("http://acme.com/foo", "urn:barNamespace")) {
      assertTrue("Generated XML has missing XML namespace declaration " + ns, newText.contains(ns))
    }
  }

}