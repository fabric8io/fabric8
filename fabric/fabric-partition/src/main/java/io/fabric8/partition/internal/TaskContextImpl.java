/*
 * Copyright 2010 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.partition.internal;

import io.fabric8.partition.TaskContext;

import java.util.Collections;
import java.util.Map;

public class TaskContextImpl implements TaskContext {

    private final String id;
    private final Map<String, ?> configuration;

    public TaskContextImpl(String id, Map<String, ?> configuration) {
        this.id = id;
        this.configuration =  Collections.unmodifiableMap(configuration);
    }

    public String getId() {
        return id;
    }

    public Map<String, ?> getConfiguration() {
        return configuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskContextImpl that = (TaskContextImpl) o;

        if (configuration != null ? !configuration.equals(that.configuration) : that.configuration != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }
}
