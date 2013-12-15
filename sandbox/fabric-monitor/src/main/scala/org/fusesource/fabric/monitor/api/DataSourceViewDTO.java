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
import java.util.Arrays;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class DataSourceViewDTO {

    @JsonProperty
    @XmlAttribute
    public String id;

    @JsonProperty
    @XmlAttribute
    public String label;

    @JsonProperty
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
    @JsonProperty
    @XmlAttribute
    public String consolidation;

    @JsonProperty
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
