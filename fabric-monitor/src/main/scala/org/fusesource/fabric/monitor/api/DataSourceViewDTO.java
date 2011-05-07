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
@XmlAccessorType(XmlAccessType.FIELD)
public class DataSourceViewDTO {

    @XmlAttribute
    public String id;

    @XmlAttribute
    public String label;

    @XmlAttribute
    public String description;

    /**
     * AVERAGE: The average of the data points is stored.
     * MIN: The smallest of the data points is stored.
     * MAX: The largest of the data points is stored.
     * LAST: The last data point is used.
     * FIRST: The fist data point is used.
     * TOTAL: The total of the data points is stored.
     */
    @XmlAttribute
    public String consolidation;

    @XmlElement(name="data")
    public double data[] = new double[]{};

}
