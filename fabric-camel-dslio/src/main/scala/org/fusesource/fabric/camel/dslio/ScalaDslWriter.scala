/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.camel.dslio

import java.io.PrintWriter
import org.apache.camel.model.{RouteDefinition, FromDefinition}

class ScalaDslWriter(out: PrintWriter) extends JavaDslWriter(out){
  var blocks = 0

  override def write(node: FromDefinition) = {
    val uri = node.getUri
    if (uri != null && uri.size > 0) {
      indent
      print(doubleQuote(uri) + " ==> ")
      startBlock()
    }
  }


  override def write(route: RouteDefinition) {
    super.write(route)
    for (i <- 0 to blocks) {
      endBlock
    }
    blocks = 0
  }

  def startBlock() {
    println(" {")
    indentLevel += 1
    blocks += 1
  }

  def endBlock() {
    indentLevel -= 1
    indent
    println("}")
  }

  override protected def parametersText(parameters: List[String]) = {
    if (parameters.isEmpty) {
      ""
    } else {
      super.parametersText(parameters)
    }
  }
}
