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
package io.fabric8.webui.agents.camel

import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.camel.facade.mbean._
import io.fabric8.camel.facade.mbean.{CamelBrowsableEndpointMBean, CamelEndpointMBean}
import javax.ws.rs._
import core.MediaType
import io.fabric8.webui.{BaseResource, HasID}
import io.fabric8.webui.{HasID, BaseResource}

class EndpointResource(val mbean: CamelEndpointMBean) extends BaseResource with HasID {

  @JsonProperty
  def kind = "simple"

  @JsonProperty
  def id = mbean.getId

  @JsonProperty
  def endpoint_uri = mbean.getEndpointUri

  def eat_exceptions[T <: AnyRef](func: => T): T = {
    try {
      func
    } catch {
      case e: Exception =>
        e.printStackTrace()
        null.asInstanceOf[T]
    }
  }

  @JsonProperty
  def state = eat_exceptions(mbean.getState)

}

class BrowsableEndpointResource(override val mbean: CamelBrowsableEndpointMBean) extends EndpointResource(mbean) {

  @JsonProperty
  override def kind = "browsable"

  @JsonProperty
  def queue_size = mbean.queueSize

  @GET
  @Path("exchanges/{index}")
  def exchanges(@PathParam("index") index: Int) = mbean.browseExchange(index)

  @GET
  @Path("messages")
  @Produces(Array(MediaType.APPLICATION_XML))
  def messages_ranged(@QueryParam("from") from: java.lang.Integer, @QueryParam("to") to: java.lang.Integer, @QueryParam("body") body: Boolean) = {
    if (from == null || to == null) {
      mbean.browseAllMessagesAsXml(body)
    } else {
      mbean.browseRangeMessagesAsXml(from.intValue(), to.intValue(), body)
    }
  }

  @GET
  @Produces(Array(MediaType.APPLICATION_XML))
  @Path("messages/{index}")
  def browse_message_as_xml(@PathParam("index") index: Int, @QueryParam("body") body: Boolean) = mbean.browseMessageAsXml(index, body)

  @GET
  @Produces(Array(MediaType.APPLICATION_OCTET_STREAM))
  @Path("messasges/{index}/body")
  def browse_message_body(@PathParam("index") index: Int) = mbean.browseMessageBody(index)


}
