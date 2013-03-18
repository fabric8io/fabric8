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
package org.fusesource.insight.metrics.model;

import java.util.Set;

public class Query {

    private final String name;
    private final Set<Request> requests;
    private final String template;
    private final String metadata;
    private final int period;
    private final int minPeriod;
    private final String lock;

    public Query(String name, Set<Request> requests, String template, String metadata, String lock, int period, int minPeriod) {
        this.name = name;
        this.requests = requests;
        this.template = template;
        this.metadata = metadata;
        this.period = period;
        this.minPeriod = minPeriod;
        this.lock = lock;
    }

    public String getName() {
        return name;
    }

    public Set<Request> getRequests() {
        return requests;
    }

    public String getTemplate() {
        return template;
    }

    public String getMetadata() {
        return metadata;
    }

    public int getPeriod() {
        return period;
    }

    public int getMinPeriod() {
        return minPeriod;
    }

    public String getLock() {
        return lock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Query query = (Query) o;

        if (period != query.period) return false;
        if (minPeriod != query.minPeriod) return false;
        if (requests != null ? !requests.equals(query.requests) : query.requests != null) return false;
        if (name != null ? !name.equals(query.name) : query.name != null) return false;
        if (template != null ? !template.equals(query.template) : query.template != null) return false;
        if (metadata != null ? !metadata.equals(query.metadata) : query.metadata != null) return false;
        if (lock != null ? !lock.equals(query.lock) : query.lock != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (requests != null ? requests.hashCode() : 0);
        result = 31 * result + (template != null ? template.hashCode() : 0);
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        result = 31 * result + period;
        return result;
    }
}
