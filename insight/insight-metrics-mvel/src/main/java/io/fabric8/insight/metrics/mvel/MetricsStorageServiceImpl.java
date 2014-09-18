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
package io.fabric8.insight.metrics.mvel;

import io.fabric8.insight.metrics.model.Metrics;
import io.fabric8.insight.metrics.model.MetricsStorageService;
import io.fabric8.insight.metrics.model.QueryResult;
import io.fabric8.insight.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link MetricsStorageService} using the JSON {@link StorageService}
 */
public class MetricsStorageServiceImpl implements MetricsStorageService {
    private static final transient Logger LOG = LoggerFactory.getLogger(MetricsStorageServiceImpl.class);

    private StorageService storageService;
    private Renderer renderer = new Renderer();

    public MetricsStorageServiceImpl() {
    }

    public MetricsStorageServiceImpl(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public void store(String type, long timestamp, QueryResult qrs) {
        String output = null;
        try {
            output = renderer.render(qrs);
        } catch (Exception e) {
            LOG.warn("Failed to render " + qrs + " to JSON: " + e, e);
        }

        if (output == null || output.trim().isEmpty()) {
            return;
        }
        String name = Metrics.metricId(type, qrs);
        storageService.store(name,
                timestamp,
                output);
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public void setStorageService(StorageService storageService) {
        this.storageService = storageService;
    }
}
