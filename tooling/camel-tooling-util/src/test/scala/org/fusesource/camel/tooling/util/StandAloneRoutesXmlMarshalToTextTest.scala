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

package org.fusesource.camel.tooling.util

import java.io.File
import org.fusesource.scalate.util.IOUtil
import java.{util => ju}
import org.apache.camel.model.RouteDefinition

class StandAloneRoutesXmlMarshalToTextTest extends RouteXmlTestSupport {

  test("marshal to Text test") {
    val text = IOUtil.loadTextFile(new File(baseDir, "src/test/resources/routes.xml"))

    val route = new RouteDefinition()
    route.from("seda:new.in").to("seda:new.out")
    val list = new ju.ArrayList[RouteDefinition]()
    list.add(route)

    val actual = tool.marshalToText(text, list)
    println("Got " + actual)

    assert(actual.contains("seda:new.in"), "Missing seda:new.in for: " + actual)
  }

}