/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.monitor.api;


import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="monitored_view")
@XmlAccessorType(XmlAccessType.FIELD)
public class MonitoredViewDTO {

    @XmlAttribute
    public long start;

    @XmlAttribute
    public long end;

    @XmlAttribute
    public long step;

    @XmlElement(name="data_source")
    public List<DataSourceViewDTO> data_sources = new ArrayList<DataSourceViewDTO>();

}
