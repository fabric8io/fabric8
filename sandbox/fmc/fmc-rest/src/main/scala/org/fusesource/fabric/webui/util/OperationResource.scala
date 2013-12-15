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
package io.fabric8.webui.util

import javax.ws.rs.core.MediaType
import com.sun.jersey.api.view.Viewable
import javax.ws.rs.{Path, GET, Produces, POST}
import javax.xml.bind.annotation.XmlRootElement
import io.fabric8.webui.BaseResource

/**
 * Resource which executes an operation. Operation is defined in unit to minimize number of instances.
 *
 * @author ldywicki
 */
@XmlRootElement()
class OperationResource(private val _model: AnyRef, id: String, operation: => Unit) extends BaseResource {

  def this() = this(null, null, null)

  /**
   * Execute operation.
   */
  @POST
  @Produces(Array(MediaType.TEXT_HTML))
  def execute(): StatusResource = {
    try {
      operation
      new StatusResource(_model, true)
    } catch {
      case _ => new StatusResource(_model, false)
    }
  }

  /**
   * Render confirmation form.
   */
  @GET
  @Produces(Array(MediaType.TEXT_HTML))
  def render() = new Viewable(id, model, model.getClass)

  // utility methods for views
  def model() = _model

}
