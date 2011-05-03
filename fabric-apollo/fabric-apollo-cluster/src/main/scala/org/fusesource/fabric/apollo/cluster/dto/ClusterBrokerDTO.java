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

import org.apache.activemq.apollo.dto.AddUserHeaderDTO;
import org.apache.activemq.apollo.dto.BrokerDTO;
import org.apache.activemq.apollo.dto.ProtocolDTO;
import org.apache.activemq.apollo.dto.RouterDTO;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Allow you to customize the stomp protocol implementation.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="cluster_broker")
@XmlAccessorType(XmlAccessType.FIELD)
public class ClusterBrokerDTO extends BrokerDTO {

    /**
     * The address cluster members should use to connect to this
     * broker.
     */
    @XmlAttribute(name="cluster_address")
    public String cluster_address;

    /**
     * The weight of the weight in the cluster.  Load is
     * partitioned in the cluster based on weight.  The higher
     * a node's weight the more load that will be routed to it.
     */
    @XmlAttribute(name="cluster_weight")
    public Integer cluster_weight;

    /**
     * Tokens the brokers use to verify that
     * the cluster peers are actually part of the cluster.
     */
    @XmlElement(name="security_token")
    public List<String> security_tokens = new ArrayList<String>();
}
