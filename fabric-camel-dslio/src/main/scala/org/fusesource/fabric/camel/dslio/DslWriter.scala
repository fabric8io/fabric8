/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.camel.dslio

import collection.JavaConversions._
import java.io.PrintWriter
import java.{util => ju}
import org.apache.camel.model._

/**
 * Base class for generating DSL output from a Camel AST model
 */
class DslWriter(out: PrintWriter) {

  def write(routes: RoutesDefinition): Unit = {
    write(routes.getRoutes)
  }

  def write(routeList: ju.List[RouteDefinition]): Unit = {
    for (route <- routeList) {
      write(route)
    }
  }

  def write(route: RouteDefinition): Unit = {
    for (from <- route.getInputs) {
      write(from)
    }
    for (out <- route.getOutputs) {
      write(route, out)
    }
  }

  def write(from: FromDefinition): Unit = {
  }

  def write(parent: ProcessorDefinition[_], definition: ProcessorDefinition[_]): Unit = {
    definition match {
      case node: ToDefinition => write(node)
      case node: FilterDefinition => write(node)
      case _ => throw new UnsupportedOperationException("No Support yet for " + definition.getShortName)
    }
    writeOutputs(definition)
  }

  def writeOutputs(definition: ProcessorDefinition[_]): Unit = {
    for (out <- definition.getOutputs) {
      write(definition, out)
    }
  }

  def write(toDefinition: ToDefinition): Unit = {
  }

  def write(filterDefinition: FilterDefinition): Unit = {
  }


}
