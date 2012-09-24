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
import org.fusesource.scalate.test.FunSuiteSupport
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.junit.Assert._
import org.fusesource.scalate.util.IOUtil

abstract class RouteXmlTestSupport extends FunSuiteSupport {

  val tool = new RouteXml

  def outDir = new File(baseDir, "target/test-" + getClass.getSimpleName)

  protected def assertLoadModel(file: File, expected: Int): XmlModel = {
    println("Loading file: " + file)
    assertFileExists(file)

    val x = tool.unmarshal(file)

    println("Got: " + x)

    val routes = x.routeDefinitions

    expect(expected, "routes: " + routes) {
      routes.size
    }

    println("routes: " + routes)
    x
  }

  protected def assertRoutes(file: File, expected: Int, ns: String = CamelNamespaces.springNS): XmlModel = {
    val x: XmlModel = assertLoadModel(file, expected)

    // now lets add a route and write it back again...
    val tmpContext = new DefaultCamelContext
    tmpContext.addRoutes(new RouteBuilder {
      def configure {
        from("seda:newFrom").to("seda:newTo")
      }
    })
    x.contextElement.getRoutes.addAll(tmpContext.getRouteDefinitions)

    val routes = x.routeDefinitions
    expect(expected + 1, "routes: " + routes) {
      routes.size
    }

    // now lets write to XML      model
    outDir.mkdirs
    val outFile = new File(outDir, file.getName)
    println("Generating file: " + outFile)
    //tool.marshal(outFile, x.contextElement)
    tool.marshal(outFile, x)

    assertFileExists(outFile)

    // lets check the file has the correct namespace inside it
    val text = IOUtil.loadTextFile(outFile)
    assert(text.contains(ns), "Namespace " + ns + " not present in output file\n" + text)
    x
  }

  protected def assertFileExists(file: File): Unit = {
    assertTrue("file should exist: " + file, file.exists)
  }


  protected def assertValid(x: XmlModel): Unit = {
    val handler = x.validate
    val errors = handler.errors
    assert(errors.size == 0, "errors were: " + errors)

  }
}