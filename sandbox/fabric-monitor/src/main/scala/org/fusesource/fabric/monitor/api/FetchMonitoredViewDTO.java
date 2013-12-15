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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="fetch_monitored_view")
@XmlAccessorType(XmlAccessType.FIELD)
public class FetchMonitoredViewDTO {

    @JsonProperty
    @XmlElement(name="monitored_set")
    public String monitored_set;

    @JsonProperty
    @XmlElement(name="data_source")
    public List<String> data_sources = new ArrayList<String>();

    @JsonProperty
    @XmlElement(name="consolidation")
    public Set<String> consolidations = new LinkedHashSet<String>();

    @JsonProperty
    @XmlAttribute
    public long start;

    @JsonProperty
    @XmlAttribute
    public long end;

    @JsonProperty
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
