/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.monitor.api;


import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.*;

/**
 * Represents a kind of value which is to be polled along with its polling mechanism (such as a Process/System/JMX value etc)
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="data_source")
@XmlAccessorType(XmlAccessType.FIELD)
public class DataSourceDTO {

    @JsonProperty
    @XmlAttribute
    public String id;

    @JsonProperty
    @XmlAttribute
    public String name;

    @JsonProperty
    @XmlAttribute
    public String description;

    /**
     * Should be one of the following:
     *
     * <ul>
     * <li>
     *      GAUGE: Is for things like temperatures or number of people in
     *      a room or the amount of memory used.
     * </li><li>
     *      COUNTER: Is for continuous incrementing counters like the
     *      number of messages that have been dispatched to a queue
     * </li><li>
     *      DERIVE: Is for counters which can go up and down; so for things like
     *      the pending messages on a queue
     * </li><li>
     *      ABSOLUTE: Is for counters which get reset upon reading.
     * </li>
     */
    @JsonProperty
    @XmlAttribute
    public String kind;

    @JsonProperty
    @XmlAttribute
    public String heartbeat;

    @JsonProperty
    @XmlAttribute
    public double min = Double.NaN;

    @JsonProperty
    @XmlAttribute
    public double max = Double.NaN;

    /**
     * How we get the data...
     */
    @JsonProperty
    @XmlElement
    public PollDTO poll;

    public DataSourceDTO() {
    }

    public DataSourceDTO(String id, String name, String description, String kind, String heartbeat, double min, double max, PollDTO poll) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.kind = kind;
        this.heartbeat = heartbeat;
        this.min = min;
        this.max = max;
        this.poll = poll;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataSourceDTO that = (DataSourceDTO) o;

        if (Double.compare(that.max, max) != 0) return false;
        if (Double.compare(that.min, min) != 0) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (heartbeat != null ? !heartbeat.equals(that.heartbeat) : that.heartbeat != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (kind != null ? !kind.equals(that.kind) : that.kind != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (poll != null ? !poll.equals(that.poll) : that.poll != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
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
