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

import com.sun.jersey.api.view.Viewable
import javax.ws.rs._
import javax.ws.rs.core.MediaType

/**
 * Marker interface for resources which are updateable.
 *
 * @author ldywicki
 */
trait Update {

  @GET
  @Path("edit")
  @Produces(Array[String](MediaType.TEXT_HTML))
  def renderUpdate(): Viewable = new Viewable("update", this, getClass)

}

/**
 * Marker interface for resources which supports creating new instances of resource.
 *
 * @author ldywicki
 */
trait Create {

  @GET
  @Path("create")
  @Produces(Array[String](MediaType.TEXT_HTML))
  def renderCreate(): Viewable = new Viewable("create", this, this.getClass)

}

/**
 * Marker interface for resources which supports removal operation.
 *
 * @author ldywicki
 */
trait Remove {

  @GET
  @Path("remove")
  @Produces(Array[String](MediaType.TEXT_HTML))
  def renderRemove(): Viewable = new Viewable("remove", this, this.getClass)

}
