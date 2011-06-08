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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="cluster_node")
@XmlAccessorType(XmlAccessType.FIELD)
public class ClusterNodeDTO {

    /**
     */
    @XmlAttribute
    public String id;

    /**
     */
    @XmlAttribute
    public int weight;

    /**
     */
    @XmlAttribute
    public String cluster_address;

    /**
     */
    @XmlAttribute
    public String client_address;



}
