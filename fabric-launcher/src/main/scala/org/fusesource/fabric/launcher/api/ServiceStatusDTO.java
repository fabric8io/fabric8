/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.launcher.api;


import org.fusesource.fabric.api.monitor.MonitoredSetDTO;

import javax.xml.bind.annotation.*;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="service_status")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceStatusDTO {

    @XmlAttribute
    public String id;

    @XmlAttribute
    public boolean enabled;

    @XmlAttribute
    public String state;

    @XmlAttribute(name="state_age")
    public long state_age;

    @XmlAttribute
    public Long pid;

    @XmlElement
    public MonitoredSetDTO monitors;

}
