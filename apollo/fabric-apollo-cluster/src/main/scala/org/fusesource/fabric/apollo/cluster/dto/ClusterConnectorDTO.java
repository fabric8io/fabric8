/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.cluster.dto;

import org.apache.activemq.apollo.dto.ConnectorTypeDTO;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "connector")
@XmlAccessorType(XmlAccessType.FIELD)
public class ClusterConnectorDTO extends ConnectorTypeDTO {

  @XmlAttribute(name="zk_url")
  public String zk_url;

  @XmlAttribute(name="zk_directory")
  public String zk_directory;

  @XmlAttribute(name="zk_timeout")
  public String zk_timeout;

  /**
   * The address cluster members should use to connect to this
   * broker.
   */
  @XmlAttribute(name="address")
  public String address;

  /**
   * The id of this broker node in the cluster.
   */
  @XmlAttribute(name="node_id")
  public String node_id;

  /**
   * The weight of the weight in the cluster.  Load is
   * partitioned in the cluster based on weight.  The higher
   * a node's weight the more load that will be routed to it.
   */
  @XmlAttribute(name="weight")
  public Integer weight;

  /**
   * Tokens the brokers use to verify that
   * the cluster peers are actually part of the cluster.
   */
  @XmlElement(name="security_token")
  public List<String> security_tokens = new ArrayList<String>();

}
