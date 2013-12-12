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
 * Represents how a monitored value is archived in RRD tool databases
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="archive")
@XmlAccessorType(XmlAccessType.FIELD)
public class ArchiveDTO {


    /**
     * AVERAGE: The average of the data points is stored.
     * MIN: The smallest of the data points is stored.
     * MAX: The largest of the data points is stored.
     * LAST: The last data point is used.
     * FIRST: The fist data point is used.
     * TOTAL: The total of the data points is stored.
     */
    @JsonProperty
    @XmlAttribute
    public String consolidation;

    /**
     *
     */
    @JsonProperty
    @XmlAttribute
    public double xff = 0.5;

    @JsonProperty
    @XmlAttribute
    public String step;

    @JsonProperty
    @XmlAttribute
    public String window;

    public ArchiveDTO() {
    }

    public ArchiveDTO(String consolidation, String step, String window) {
        this.consolidation = consolidation;
        this.step = step;
        this.window = window;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArchiveDTO that = (ArchiveDTO) o;

        if (Double.compare(that.xff, xff) != 0) return false;
        if (consolidation != null ? !consolidation.equals(that.consolidation) : that.consolidation != null)
            return false;
        if (step != null ? !step.equals(that.step) : that.step != null)
            return false;
        if (window != null ? !window.equals(that.window) : that.window != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = consolidation != null ? consolidation.hashCode() : 0;
        temp = xff != +0.0d ? Double.doubleToLongBits(xff) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (step != null ? step.hashCode() : 0);
        result = 31 * result + (window != null ? window.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ArchiveDTO{" +
                "consolidation='" + consolidation + '\'' +
                ", xff=" + xff +
                ", step=" + step +
                ", total=" + window +
                '}';
    }
}
