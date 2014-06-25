/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.insight.rhq.metrics;

import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.common.util.Strings;
import io.fabric8.insight.metrics.model.MBeanAttrResult;
import io.fabric8.insight.metrics.model.MBeanAttrsResult;
import io.fabric8.insight.metrics.model.MBeanOperResult;
import io.fabric8.insight.metrics.model.MBeanOpersResult;
import io.fabric8.insight.metrics.model.Metrics;
import io.fabric8.insight.metrics.model.MetricsStorageService;
import io.fabric8.insight.metrics.model.QueryResult;
import io.fabric8.insight.metrics.model.Result;
import io.fabric8.insight.storage.StorageService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Service;
import org.rhq.metrics.RHQMetrics;
import org.rhq.metrics.core.RawNumericMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of {@link StorageService} using <a href="https://github.com/rhq-project/rhq-metrics">RHQ Metrics</a>
 */
@ThreadSafe
@Component(name = "io.fabric8.insight.rhq.metrics", label = "Fabric8 RHQ Metrics Storage", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = false)
@Service(MetricsStorageService.class)
public class RhqMetricsStorage extends AbstractComponent implements MetricsStorageService {
    private static final transient Logger LOG = LoggerFactory.getLogger(RhqMetricsStorage.class);

    private RHQMetrics metricsService;

    @Activate
    void activate(Map<String, String> configuration) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        updateConfiguration(configuration);
        activateComponent();
    }

    @Modified
    void modified(Map<String, String> configuration) {
        updateConfiguration(configuration);
    }

    @Deactivate
    void deactivate() throws MBeanRegistrationException, InstanceNotFoundException {
        deactivateComponent();
        shutdownMetricService();
    }

    protected void updateConfiguration(Map<String, String> configuration) {
        shutdownMetricService();
        metricsService = createMetricService(configuration);
        LOG.info("Created metrics service " + metricsService);
    }

    protected void shutdownMetricService() {
        if (metricsService != null) {
            try {
                LOG.info("Shutting down metrics service " + metricsService);
                metricsService.shutdown();
            } finally {
                metricsService = null;
            }
        }
    }

    protected RHQMetrics createMetricService(Map<String, String> configuration) {
        RHQMetrics.Builder builder = new RHQMetrics.Builder();

        // TODO on next release should do this...
        //builder = builder.withOptions(configuration);

        // enable Cassandra discovery if there are any nodes available
        if (Strings.isNotBlank(configuration.get("nodes"))) {
            builder = builder.withCassandraDataStore();
        }
        return builder.build();
    }


    @Override
    public void store(String type, long timestamp, QueryResult queryResult) {
        assertValid();
        if (metricsService == null) {
            throw new IllegalStateException("No metricsService available!");
        }
        Map<String, Result<?>> results = queryResult.getResults();
        if (results != null) {
            Set<RawNumericMetric> data = new HashSet<>();
            Set<Map.Entry<String, Result<?>>> entries = results.entrySet();
            for (Map.Entry<String, Result<?>> entry : entries) {
                String key = entry.getKey();
                Result<?> result = entry.getValue();
                if (result instanceof MBeanOpersResult) {
                    MBeanOpersResult opersResult = (MBeanOpersResult) result;
                    List<MBeanOperResult> operResults = opersResult.getResults();
                    if (operResults != null) {
                        for (MBeanOperResult operResult : operResults) {
                            Object value = operResult.getValue();
                            Double doubleValue = toDouble(value);
                            if (doubleValue != null) {
                                String id = Metrics.metricId(type, opersResult.getRequest());
                                data.add(new RawNumericMetric(id, doubleValue, timestamp));
                            }
                        }
                    }
                } else if (result instanceof MBeanAttrsResult) {
                    MBeanAttrsResult attrsResult = (MBeanAttrsResult) result;
                    List<MBeanAttrResult> attrResults = attrsResult.getResults();
                    if (attrResults != null) {
                        for (MBeanAttrResult attrResult : attrResults) {
                            Map<String, Object> attrs = attrResult.getAttrs();
                            if (attrs != null) {
                                Set<Map.Entry<String, Object>> attrEntries = attrs.entrySet();
                                for (Map.Entry<String, Object> attrEntry : attrEntries) {
                                    String attributeName = attrEntry.getKey();
                                    Object value = attrEntry.getValue();
                                    Double doubleValue = toDouble(value);
                                    if (doubleValue != null) {
                                        String id = Metrics.metricId(type, attrsResult.getRequest(), attributeName);
                                        data.add(new RawNumericMetric(id, doubleValue, timestamp));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (!data.isEmpty()) {
                metricsService.addData(data);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("added " + data.size() + " metrics");
                }
            }
        }
    }

    protected Double toDouble(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            Number number = (Number) value;
            return number.doubleValue();
        } else {
            return null;
        }
    }


}
