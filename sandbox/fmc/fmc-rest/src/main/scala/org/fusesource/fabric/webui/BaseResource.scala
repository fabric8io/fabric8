/*
 * Copyright 2010 Red Hat, Inc.
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
package io.fabric8.webui

import org.slf4j.{LoggerFactory, Logger}
import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import javax.ws.rs._
import core.Response.Status._
import javax.ws.rs.core.{Response, UriInfo, Context, MediaType}
import javax.servlet.http.{HttpSession, HttpServletResponse, HttpServletRequest}
import javax.ws.rs.core.Response.Status
import org.codehaus.jackson.annotate.{JsonMethod, JsonAutoDetect}
import io.fabric8.api.Container
import java.io.{PipedInputStream, PipedOutputStream, OutputStream}
import concurrent.ops._
import sun.management.resources.agent
import javax.security.auth.Subject

trait HasID {
  def id: String
}

object ByID {
  def apply(a: HasID, b: HasID): Boolean = a.id.compareToIgnoreCase(b.id) < 0
}

trait Exportable {

  def do_export(name: String) = {
    val sink = new PipedOutputStream
    val source = new PipedInputStream(sink)

    spawn {
      export(sink)
    }
    source
  }


  def export(sink: OutputStream): Unit
}

/**
 * Defines the default representations to be used on resources
 */
@JsonAutoDetect(Array(JsonMethod.NONE))
@Produces(Array(MediaType.APPLICATION_JSON))
class BaseResource {

  def fabric_service = Services.fabric_service

  def agent_template(agent: Container, jmx_username: String, jmx_password: String) =
    Services.agent_template(agent, jmx_username, jmx_password)

  @Context
  protected val uriInfo: UriInfo = null

  @Context
  var request:HttpServletRequest = _

  protected val logger: Logger = LoggerFactory getLogger getClass

  /**
   * This method will allow returning this resource as data.
   */
  @GET
  def get: Any = {
    this
  }

  def subject:Subject = {
    Services.get_subject(request) match {
      case Some(subject) =>
        subject
      case None =>
        no_permission
    }
  }

  /**
   * Utility method to execute code and wrap result in Map.
   */
  protected def status(code: Unit): java.util.Map[String, Any] = {
    val status: Map[String, Any] = Map()
    try {
      code;
      status + ("status" -> true)
    } catch {
      case e: Throwable =>
        status + ("status" -> false)
        status + ("message" -> e.getMessage)
    }
    status
  }

  def respond[T](value: Status, message: Any = null): T = {
    val response = Response.status(value)
    if (message != null) {
      response.entity(message)
    }
    throw new WebApplicationException(response.build)
  }

  protected def redirect(path: String, rq: HttpServletRequest, rp: HttpServletResponse) = {
    Response.ok.build
    rp.sendRedirect(rq.getContextPath + path)
    null
  }

  protected def no_permission[T]: T = throw new WebApplicationException(Response.Status.FORBIDDEN)

  protected def not_found[T]: T = throw new WebApplicationException(Response.Status.NOT_FOUND)

  implicit def iter[T](some: java.util.Collection[T]) = {
    collection.JavaConversions.asScalaIterator(some.iterator())
  }

}
