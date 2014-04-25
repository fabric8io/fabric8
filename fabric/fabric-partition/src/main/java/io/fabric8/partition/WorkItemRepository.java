/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.partition;

import java.net.URL;
import java.util.List;

public interface WorkItemRepository {

    /**
     * Start or resume the repository.
     * This method will activate the repository and will notify listeners of changes.
     */
    void start();

    /**
     * Suspends the repository.
     * When the repository is listeners will not get notified of changes.
     */
    void stop();

    /**
     * Completely shuts down the repository. Cannot be resumed.
     */
    void close();

    List<String> listWorkItemLocations();

    WorkItem readWorkItem(String location);

    void notifyListeners();

    void addListener(WorkItemListener workItemListener);

    void removeListener(WorkItemListener workItemListener);

}
