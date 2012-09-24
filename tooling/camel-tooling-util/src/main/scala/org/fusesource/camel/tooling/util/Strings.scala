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

import java.beans.Introspector


object Strings {

  def isEmpty(text: String) = text == null || text.length == 0
  
  def splitCamelCase(text: String) = {
    val buffer = new StringBuilder
    var last = 'A'
    for (c <- text) {
      if (last.isLower && c.isUpper) {
        buffer.append(" ")
      }
      buffer.append(c)
      last = c
    }
    buffer.toString
  }

  def capitalize(text: String) = text.capitalize

  def decapitalize(text: String) = Introspector.decapitalize(text)

  def toJson(v: Any) = v match {
      case null => "null"
      case n: Number => n
      case s: String => "\"" + s.replaceAll("\\n", "\\\\n")  + "\""
      case s => "\"" + s + "\""
  }
}