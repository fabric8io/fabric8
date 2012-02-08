/**
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

package org.fusesource.fabric.apollo.cluster.dto;

import org.fusesource.fabric.groups.NodeState;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name = "cluster_node")
@XmlAccessorType(XmlAccessType.FIELD)
public class ClusterNodeDTO implements NodeState {

  /**
   * The id of the cluster node.  There can be multiple node with this ID,
   * but only the first node in the cluster will be the master for for it.
   */
  @XmlAttribute
  public String id;

  /**
   * The weight of the node in the cluster.  The higher the weight the
   * more work that will get assigned to the node.
   */
  @XmlAttribute
  public int weight;

  /**
   * The address other cluster nodes should use to connect to the node.
   */
  @XmlAttribute(name = "cluster_address")
  public String cluster_address;

  /**
   * The address clients should use to connect to the node.
   */
  @XmlAttribute(name = "cluster_address")
  public String client_address;

  /**
   */
  public String id() {
    return id;
  }
}
