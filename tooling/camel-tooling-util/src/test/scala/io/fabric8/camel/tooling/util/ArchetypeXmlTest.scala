/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.camel.tooling.util

import collection.JavaConversions._

import org.fusesource.scalate.test.FunSuiteSupport
import java.io.{StringReader, StringWriter}

class ArchetypeXmlTest extends FunSuiteSupport {
  test("marshal archetypes") {

    val archetypes = new Archetypes()
    archetypes.add(Archetype("foo", "bar", "1.1", "Some description"))
    archetypes.add(Archetype("xyz", "whatever", "2.3", "Docs..."))

    val buffer = new StringWriter()
    Archetypes.newMarshaller().marshal(archetypes, buffer)

    println("Generated XML: " + buffer)

    Archetypes.newUnmarshaller().unmarshal(new StringReader(buffer.toString)) match {
      case actual: Archetypes =>
        expect(2) { actual.archetypes.size }
        for (a <- actual.archetypes) {
          println(a)
        }
      case x => fail("Found " + x + " when expected an Archetypes")
    }
  }
}
