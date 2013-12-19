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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.fabric8.api.FabricException;
import io.fabric8.partition.WorkItemListener;
import io.fabric8.partition.WorkItemRepository;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class BaseWorkItemRepository implements WorkItemRepository {

    final List<WorkItemListener> workItemListeners = new CopyOnWriteArrayList<WorkItemListener>();

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
}
