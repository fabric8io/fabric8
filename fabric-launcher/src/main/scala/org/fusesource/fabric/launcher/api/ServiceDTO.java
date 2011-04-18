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


import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="service")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceDTO {

    @XmlAttribute
    public String id;

    @XmlAttribute
    public Boolean enabled;

    @XmlElement
    public String notes;

    @XmlElement(name="start")
    public List<String> start = new ArrayList<String>();

    @XmlElement(name="stop")
    public List<String> stop = new ArrayList<String>();

    @XmlElement(name="pid_file")
    public String pid_file;


}
