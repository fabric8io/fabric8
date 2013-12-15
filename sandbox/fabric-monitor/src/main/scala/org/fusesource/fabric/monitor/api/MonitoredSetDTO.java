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

    @JsonProperty
    @XmlAttribute
    public String name;

    @JsonProperty
    @XmlAttribute
    public String step = "1s";

    @JsonProperty
    @XmlElement(name="data_source")
    public List<DataSourceDTO> data_sources = new ArrayList<DataSourceDTO>();

    @JsonProperty
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
