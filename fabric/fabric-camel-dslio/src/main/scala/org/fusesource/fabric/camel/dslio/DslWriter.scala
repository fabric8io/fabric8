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
