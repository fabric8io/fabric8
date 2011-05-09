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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="fetch_monitored_view")
@XmlAccessorType(XmlAccessType.FIELD)
public class FetchMonitoredViewDTO {

    @XmlElement(name="monitored_set")
    public String monitored_set;

    @XmlElement(name="data_source")
    public List<String> data_sources = new ArrayList<String>();

    @XmlElement(name="consolidation")
    public Set<String> consolidations = new LinkedHashSet<String>();

    @XmlAttribute
    public long start;

    @XmlAttribute
    public long end;

    @XmlAttribute
    public long step;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FetchMonitoredViewDTO that = (FetchMonitoredViewDTO) o;

        if (end != that.end) return false;
        if (start != that.start) return false;
        if (step != that.step) return false;
        if (consolidations != null ? !consolidations.equals(that.consolidations) : that.consolidations != null)
            return false;
        if (data_sources != null ? !data_sources.equals(that.data_sources) : that.data_sources != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = data_sources != null ? data_sources.hashCode() : 0;
        result = 31 * result + (consolidations != null ? consolidations.hashCode() : 0);
        result = 31 * result + (int) (start ^ (start >>> 32));
        result = 31 * result + (int) (end ^ (end >>> 32));
        result = 31 * result + (int) (step ^ (step >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "FetchMonitoredViewDTO{" +
                "consolidations=" + consolidations +
                ", data_sources=" + data_sources +
                ", start=" + start +
                ", end=" + end +
                ", step=" + step +
                '}';
    }
}
