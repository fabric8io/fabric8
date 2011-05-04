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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ChannelStatusDTO extends ConnectionStatusDTO {

    /**
     * What the connection is currently waiting on
     */
    @XmlAttribute(name="id")
	public long id;

    @XmlAttribute(name="byte_credits")
	public int byte_credits;

    @XmlAttribute(name="delivery_credits")
	public int delivery_credits;

    @XmlAttribute(name="connected")
	public boolean connected;
}
