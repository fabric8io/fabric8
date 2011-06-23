/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.api.monitor;


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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MonitoredViewDTO that = (MonitoredViewDTO) o;

        if (end != that.end) return false;
        if (step != that.step) return false;
        if (start != that.start) return false;
        if (data_sources != null ? !data_sources.equals(that.data_sources) : that.data_sources != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (start ^ (start >>> 32));
        result = 31 * result + (int) (end ^ (end >>> 32));
        result = 31 * result + (int) (step ^ (step >>> 32));
        result = 31 * result + (data_sources != null ? data_sources.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MonitoredViewDTO{" +
                "start=" + start +
                ", end=" + end +
                ", step=" + step +
                ", data_sources=" + data_sources +
                '}';
    }
}
