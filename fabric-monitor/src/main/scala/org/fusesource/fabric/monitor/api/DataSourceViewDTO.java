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
import java.util.Arrays;
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataSourceViewDTO that = (DataSourceViewDTO) o;

        if (consolidation != null ? !consolidation.equals(that.consolidation) : that.consolidation != null)
            return false;
        if (!Arrays.equals(data, that.data)) return false;
        if (description != null ? !description.equals(that.description) : that.description != null)
            return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (label != null ? !label.equals(that.label) : that.label != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (consolidation != null ? consolidation.hashCode() : 0);
        result = 31 * result + (data != null ? Arrays.hashCode(data) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DataSourceViewDTO{" +
                "consolidation='" + consolidation + '\'' +
                ", id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
