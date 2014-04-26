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
package io.fabric8.insight.activemq.base;

import io.fabric8.insight.storage.StorageService;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class StorageProxy implements StorageService {

    private BundleContext context;
    private ServiceTracker<StorageService, StorageService> tracker;

    public StorageProxy() {
    }

    public void setContext(BundleContext context) {
        this.context = context;
    }

    public void init() {
        this.tracker = new ServiceTracker<StorageService, StorageService>(context, StorageService.class, null);
        this.tracker.open();
    }

    public void destroy() {
        this.tracker.close();
    }

    @Override
    public void store(String type, long timestamp, String jsonData) {
        StorageService storage = this.tracker.getService();
        if (storage != null) {
            storage.store(type, timestamp, jsonData);
        }
    }

}
