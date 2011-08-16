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

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="cluster_protocol_hello")
@XmlAccessorType(XmlAccessType.FIELD)
public class ClusterProtocolHelloDTO {

    /**
     * version of the cluster protocol being used.
     */
    @XmlAttribute(name="version")
    public String version;

    /**
     * The id of the cluster node.
     */
    @XmlAttribute(name="id")
    public String id;

    /**
     * Tokens the brokers use to verify that
     * the peers are actually part of the cluster.
     */
    @XmlElement(name="security_token")
    public List<String> security_tokens = new ArrayList<String>();

    /**
     * The protocol://address:port that the peer
     * is connecting to.
     */
    @XmlAttribute(name="remote_address")
    public String remote_address;

}
