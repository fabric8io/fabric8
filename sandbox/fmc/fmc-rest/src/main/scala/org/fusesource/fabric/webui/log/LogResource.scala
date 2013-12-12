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
package io.fabric8.webui.log

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.ServletContext
import javax.ws.rs._
import core.{MediaType, Response, Context}
import java.net.{URI, HttpURLConnection, URL}
import io.fabric8.webui.{BaseResource, Services}
import java.io._
import scala.Array._
import org.apache.zookeeper.KeeperException.NoNodeException
import io.fabric8.groups.internal.ZooKeeperGroup
import io.fabric8.groups.NodeState

@Path("/log")
class LogResource extends BaseResource {

  @Context
  var servletContext: ServletContext = _

  @Context
  var response: HttpServletResponse = _


  @GET
  override def get = forward("", null)

  @Path("{path:.*}")
  @GET
  def get(@PathParam("path") path: String) = forward(path, null)

  @Path("{path:.*}")
  @DELETE
  def delete(@PathParam("path") path: String) = forward(path, null)

  @Path("{path:.*}")
  @PUT
  @Consumes(Array(MediaType.APPLICATION_JSON, MediaType.WILDCARD))
  def put(@PathParam("path") path: String, body: Array[Byte]) = forward(path, body)

  @Path("{path:.*}")
  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON, MediaType.WILDCARD))
  def post(@PathParam("path") path: String, body: Array[Byte]) = forward(path, body)


  def forward(path: String, body: Array[Byte]) = {
    val query = if (request.getQueryString != null) {
      "?" + request.getQueryString
    } else {
      ""
    }

    val base = try {
      /*
      import collection.JavaConversions
      var members : List[NodeState] = ZooKeeperGroup.members[NodeState](Services.curator, "/fabric/registry/clusters/elastic-search", Class[NodeState]).values().toList
      val urls = members.flatMap(node => node.services)

        /*
          try {
            Some(new URI(new String(data, "UTF-8")).toString)
          } catch {
            case _ => None // Perhaps it was not a URL.
          }
          */
      urls.headOption.getOrElse(not_found)
      */
      "elasticsearch"
    } catch {
      case e: NoNodeException => not_found
    }

    val url = new URL(base.stripSuffix("/") + "/" + path + query)
    val con = url.openConnection.asInstanceOf[HttpURLConnection]
    con.setUseCaches(false);
    con.setDoInput(true)

    val mth = request.getMethod
    con.setRequestMethod(mth);
    val hasBody = (mth == "POST" || mth == "PUT")
    if (hasBody) {
      con.setDoOutput(true);
    }
    con.connect
    if (hasBody) {
      copy(new ByteArrayInputStream(body), con.getOutputStream)
    }
    if (con.getResponseCode != 200) {
      val response = Response.status(con.getResponseCode)
      val message = con.getResponseMessage
      if (message != null) {
        response.entity(message)
      }
      throw new WebApplicationException(response.build)
    }
    con.getInputStream
  }

  def copy(is: InputStream, os: OutputStream) {
    try {
      val buf = new Array[Byte](8192)
      while (is.available > 0) {
        val len = is.read(buf)
        os.write(buf, 0, len)
      }
    } finally {
      close(is);
      close(os)
    }
  }

  def close(c: Closeable) {
    try {
      c.close
    } catch {
      case t =>
    }
  }
}
