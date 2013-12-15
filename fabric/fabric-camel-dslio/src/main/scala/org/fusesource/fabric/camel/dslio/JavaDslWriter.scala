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

package io.fabric8.camel.dslio

import java.io.PrintWriter
import org.apache.camel.model.language.ExpressionDefinition
import org.apache.camel.model._

class JavaDslWriter(out: PrintWriter) extends DslWriter(out) {
  def flush() = out.flush()

  import out._

  var indentText = "    "
  var indentLevel = 2

  var startedStatement = false

  def doubleQuote(text: String): String = {
    "\"" + text + "\""
  }

  override def write(from: FromDefinition) {
    val uri = from.getUri
    val args = if (uri != null) {
      List(doubleQuote(uri))
    } else {
      List()
    }
    functionStatement("from", args)
  }


  override def write(to: ToDefinition) {
    val uri = to.getUri
    val args = if (uri != null) {
      List("\"" + uri + "\"")
    } else {
      List()
    }
    functionStatement("to", args)
  }


  def writeExpression(node: ExpressionDefinition): Unit = {
    print("." + node.getLanguage + "(\"" + node.getExpression + "\")")
  }

  override def write(node: FilterDefinition): Unit = {
    functionStatement("filter")
    writeExpression(node.getExpression)
  }

  override def write(route: RouteDefinition): Unit = {
    super.write(route)
    indentLevel -= 1
    startedStatement = false
    println(";")
  }

  protected def functionStatement(name: String, parameters: List[String] = List()): Unit = {
    if (startedStatement) {
      println(".")
    }
    indent
    print(name + parametersText(parameters))
    if (!startedStatement) {
      indentLevel += 1
      startedStatement = true
    }
  }

  protected def parametersText(parameters: scala.List[String]): String = {
    parameters.mkString("(", ",", ")")
  }

  protected def indent: Unit = {
    for (i <- 0 to indentLevel) {
      print(indentText)
    }
  }

}
