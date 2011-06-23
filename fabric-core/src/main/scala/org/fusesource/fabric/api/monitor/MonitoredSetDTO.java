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
 * Defines a set of values to be monitored along with how they are to be archived
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="monitored_set")
@XmlAccessorType(XmlAccessType.FIELD)
public class MonitoredSetDTO {

    @XmlAttribute
    public String name;

    @XmlAttribute
    public String step = "1s";

    @XmlElement(name="data_source")
    public List<DataSourceDTO> data_sources = new ArrayList<DataSourceDTO>();

    @XmlElement(name="archive")
    public List<ArchiveDTO> archives = new ArrayList<ArchiveDTO>();

    public MonitoredSetDTO() {
    }

    public MonitoredSetDTO(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MonitoredSetDTO that = (MonitoredSetDTO) o;

        if (archives != null ? !archives.equals(that.archives) : that.archives != null)
            return false;
        if (data_sources != null ? !data_sources.equals(that.data_sources) : that.data_sources != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        if (step != null ? !step.equals(that.step) : that.step != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (step != null ? step.hashCode() : 0);
        result = 31 * result + (data_sources != null ? data_sources.hashCode() : 0);
        result = 31 * result + (archives != null ? archives.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MonitoredSetDTO{" +
                "archives=" + archives +
                ", name='" + name + '\'' +
                ", step=" + step +
                ", data_sources=" + data_sources +
                '}';
    }
}
