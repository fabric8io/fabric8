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

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="data_source")
@XmlAccessorType(XmlAccessType.FIELD)
public class DataSourceDTO {

    @XmlAttribute
    public String id;

    @XmlAttribute
    public String name;

    @XmlAttribute
    public String description;

    /**
     * Should be one of the following:
     *
     * <ul>
     * <li>
     *      GAUGE: Is for things like temperatures or number of people in
     *      a room or the value of a RedHat share.
     * </li><li>
     *      COUNTER: Is for continuous incrementing counters like the
     *      ifInOctets counter in a router.
     * </li><li>
     *      DERIVE: Will store the derivative of the line going from
     *      the last to the current value of the data source.
     * </li><li>
     *      ABSOLUTE: Is for counters which get reset upon reading.
     * </li>
     */
    @XmlAttribute
    public String kind;

    @XmlAttribute
    public String heartbeat;

    @XmlAttribute
    public double min = Double.NaN;

    @XmlAttribute
    public double max = Double.NaN;

    /**
     * How we get the data...
     */
    @XmlElement
    public PollDTO poll;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataSourceDTO that = (DataSourceDTO) o;

        if (Double.compare(that.max, max) != 0) return false;
        if (Double.compare(that.min, min) != 0) return false;
        if (heartbeat != null ? !heartbeat.equals(that.heartbeat) : that.heartbeat != null)
            return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (kind != null ? !kind.equals(that.kind) : that.kind != null)
            return false;
        if (poll != null ? !poll.equals(that.poll) : that.poll != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id != null ? id.hashCode() : 0;
        result = 31 * result + (kind != null ? kind.hashCode() : 0);
        result = 31 * result + (heartbeat != null ? heartbeat.hashCode() : 0);
        temp = min != +0.0d ? Double.doubleToLongBits(min) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = max != +0.0d ? Double.doubleToLongBits(max) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (poll != null ? poll.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DataSourceDTO{" +
                "description='" + description + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", kind='" + kind + '\'' +
                ", heartbeat='" + heartbeat + '\'' +
                ", min=" + min +
                ", max=" + max +
                ", poll=" + poll +
                '}';
    }
}
