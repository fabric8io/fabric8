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

import xml.{Node, Elem}

object XmlHelper {

  /**
   * Returns the integer value of the given attribute or return the default value if none is provided
   */
  def attributeIntValue(e: Node, name: String, defaultValue: Int = -1): Int = {
    e.attribute(name) match {
      case Some(s) =>
        if (s.isEmpty) {
          defaultValue
        }
      else {
          s.head.text.toInt
        }
      case _ => defaultValue
    }
  }
  /**
   * Returns the double value of the given attribute or return the default value if none is provided
   */
  def attributeDoubleValue(e: Node, name: String, defaultValue: Double = -1): Double = {
    e.attribute(name) match {
      case Some(s) =>
        if (s.isEmpty) {
          defaultValue
        }
      else {
          s.head.text.toDouble
        }
      case _ => defaultValue
    }
  }


  /**
   *  Escapes any XML special characters.
   */
  def escape(text: String): String =
    text.foldLeft(new StringBuffer)((acc, ch) => escape(ch, acc)).toString

  def unescape(text: String): String = {
    // TODO would me much more efficient to find all & first!
    var answer = text
    for ((k, v) <- encodingMap) {
      answer = answer.replaceAll(v, k.toString)
    }
    answer
  }

  protected val encodingMap = Map[Char,String]('"' -> "&quot;", '&' -> "&amp;", '<' -> "&lt;", '>' -> "&gt;")

  private def escape(ch: Char, buffer: StringBuffer): StringBuffer = {
    val s = encodingMap.getOrElse(ch, ch.toString)
    buffer.append(s)
  }

}