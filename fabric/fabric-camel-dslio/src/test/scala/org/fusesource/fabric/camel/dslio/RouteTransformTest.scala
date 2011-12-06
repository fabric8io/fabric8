/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.camel.dslio

import org.apache.camel.test.CamelTestSupport
import org.apache.camel.builder.RouteBuilder
import org.junit.Test
import java.io.{PrintWriter, StringWriter}

class RouteTransformTest extends CamelTestSupport {

  @Test
  def testWriteJavaDsl(): Unit = {
    val buffer = new StringWriter()
    val out = new PrintWriter(buffer)
    assertWriteDsl(new JavaDslWriter(out), buffer)
  }

  @Test
  def testWriteScalaDsl(): Unit = {
    val buffer = new StringWriter()
    val out = new PrintWriter(buffer)
    assertWriteDsl(new ScalaDslWriter(out), buffer)
  }

  protected def assertWriteDsl(dslWriter: JavaDslWriter, buffer: StringWriter) {
    dslWriter.write(context.getRouteDefinitions)
    dslWriter.flush()

    println("Created DSL using " + dslWriter)
    println(buffer)
  }

  override def createRouteBuilder() = {
    new RouteBuilder() {
      def configure() {
        from("seda:route1.in").
                filter().xpath("/foo/bar").
                to("seda:route1.out")
      }
    }
  }
}
