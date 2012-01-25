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

import org.apache.activemq.apollo.dto.ConnectionStatusDTO;
import org.apache.activemq.apollo.dto.VirtualHostDTO;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="cluster_connection_status")
@XmlAccessorType(XmlAccessType.FIELD)
public class ClusterConnectionStatusDTO extends ConnectionStatusDTO {

    @XmlAttribute(name="node_id")
    public String node_id;

    /**
     * What the connection is currently waiting on
     */
    @XmlAttribute(name="waiting_on")
	public String waiting_on;

    @XmlAttribute(name="exported_consumer_count")
    public int exported_consumer_count;

    @XmlAttribute(name="imported_consumer_count")
    public int imported_consumer_count;

    /**
     * The inbound channels
     */
    @XmlElement(name="inbound_channel")
    public List<ChannelStatusDTO> inbound_channels = new ArrayList<ChannelStatusDTO>();

    /**
     * The outbound channels
     */
    @XmlElement(name="outbound_channel")
    public List<ChannelStatusDTO> outbound_channels = new ArrayList<ChannelStatusDTO>();

}
