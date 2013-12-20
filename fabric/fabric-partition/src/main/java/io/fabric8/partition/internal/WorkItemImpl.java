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

import io.fabric8.partition.WorkItem;

import java.util.Map;

public class WorkItemImpl implements WorkItem {

    private final String id;
    private final String location;
    private final String content;
    private final Map<String, String> data;

    public WorkItemImpl(String id, String location, String content, Map<String, String> data) {
        this.id = id;
        this.location = location;
        this.content = content;
        this.data = data;
        this.data.put(ID_PROPERTY_NAME, id);
        this.data.put(LOCATION_PROPERTY_NAME, location);
    }

    public String getId() {
        return id;
    }

    public String getLocation() { return location;}

    public String getContent() { return content;}

    public Map<String, String> getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkItemImpl partition = (WorkItemImpl) o;

        if (id != null ? !id.equals(partition.id) : partition.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "WorkItemImpl{" +
                "id='" + id + '\'' +
                ", data=" + data +
                '}';
    }
}
