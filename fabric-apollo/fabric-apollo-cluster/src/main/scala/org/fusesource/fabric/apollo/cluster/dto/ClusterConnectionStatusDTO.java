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
