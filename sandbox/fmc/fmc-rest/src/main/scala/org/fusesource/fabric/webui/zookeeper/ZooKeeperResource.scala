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
package io.fabric8.webui.zookeeper

import javax.ws.rs.{PathParam, GET, Path}
import javax.xml.bind.annotation.{XmlElement, XmlAttribute, XmlRootElement}
import io.fabric8.webui.{Services, BaseResource}
import io.fabric8.webui.{Services, BaseResource}
import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.zookeeper.utils.ZooKeeperUtils
import org.apache.curator.framework.CuratorFramework

@Path("/zookeeper")
class ZooKeeperResource(val curator: CuratorFramework, val path: String) extends BaseResource {

  def this() = this(Services.curator, "/")

  @JsonProperty
  def getPath = path

  @JsonProperty
  def getValue = ZooKeeperUtils.getStringData(curator, path)

  @JsonProperty
  def getChildren: Array[String] = ZooKeeperUtils.getChildren(curator, path).toArray(new Array[String](0))

  @Path("{path:.*}")
  @GET
  def getChild(@PathParam("path") child: String): ZooKeeperResource = {
    val p = if (path.endsWith("/")) path else path + "/"
    new ZooKeeperResource(curator, p + child)
  }

}

