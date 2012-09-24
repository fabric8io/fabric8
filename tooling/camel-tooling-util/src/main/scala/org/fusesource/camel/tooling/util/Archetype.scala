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

import javax.xml.bind.annotation.{XmlRootElement, XmlValue, XmlAttribute, XmlElement}

object Archetype {
  def apply(groupId: String, artifactId: String, version: String, description: String = ""): Archetype = {
    val answer = new Archetype()
    answer.groupId = groupId
    answer.artifactId = artifactId
    answer.version = version
    answer.description = description
    answer
  }
}

/**
 * A simple DTO
 */
@XmlRootElement(name = "archetype")
class Archetype {
  @XmlAttribute
  var groupId: String = _
  @XmlAttribute
  var artifactId: String = _
  @XmlAttribute
  var version: String = _
  @XmlValue
  var description: String = _

  override def toString = "Archtype(" + groupId + ":" + artifactId + ":" + version + ")"
}
