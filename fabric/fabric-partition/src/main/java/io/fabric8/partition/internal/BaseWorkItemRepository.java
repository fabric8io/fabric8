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

import com.google.common.collect.Maps;
import io.fabric8.partition.WorkItem;
import io.fabric8.partition.WorkItemListener;
import io.fabric8.partition.WorkItemRepository;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class BaseWorkItemRepository implements WorkItemRepository {

    final List<WorkItemListener> workItemListeners = new CopyOnWriteArrayList<WorkItemListener>();
    final ObjectMapper mapper = new ObjectMapper();
    final TypeReference<HashMap<String, String>> partitionTypeRef = new TypeReference<HashMap<String, String>>() {
    };

    public abstract String readContent(String location);

    @Override
    public void notifyListeners() {
        for (WorkItemListener workItemListener : workItemListeners) {
            workItemListener.partitionUpdated();
        }
    }

    @Override
    public void addListener(WorkItemListener workItemListener) {
        workItemListeners.add(workItemListener);
    }

    @Override
    public void removeListener(WorkItemListener workItemListener) {
        workItemListeners.remove(workItemListener);
    }

    public WorkItem readWorkItem(String location) {
        String id = location.contains(File.separator) ? location.substring(location.lastIndexOf(File.separator) + 1) : location;
        try {
            String content = readContent(location);
            Map<String, String> data = mapper.readValue(content, partitionTypeRef);
            return new WorkItemImpl(id, location, content, data);
        } catch (Exception ex) {
            return new WorkItemImpl(id, location, "", Maps.<String,String>newHashMap());
        }

    }

}
