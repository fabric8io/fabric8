/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
