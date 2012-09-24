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

import java.{util => ju}
import javax.xml.bind.annotation.{XmlRootElement, XmlElement}
import javax.xml.bind.{Marshaller, JAXBContext}

object Archetypes {
  def newJaxbContext = {
    JAXBContext.newInstance(classOf[Archetypes], classOf[Archetype])
  }

  def newMarshaller() = {
    val m = newJaxbContext.createMarshaller()
    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    m
  }

  def newUnmarshaller() = {
    newJaxbContext.createUnmarshaller()
  }
}

@XmlRootElement(name = "archetypes")
class Archetypes {
  @XmlElement(name = "archetype")
  var archetypes: ju.List[Archetype] = new ju.ArrayList()

  def add(a: Archetype) = archetypes.add(a)
}
