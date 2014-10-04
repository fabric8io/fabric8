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
package io.fabric8.insight.rhq.metrics;

import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
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
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of {@link StorageService} using <a href="https://github.com/rhq-project/rhq-metrics">RHQ Metrics</a>
 */
@ThreadSafe
@Component(name = "io.fabric8.insight.rhq.metrics", label = "Fabric8 RHQ Metrics Storage", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service(MetricsStorageService.class)
public class RhqMetricsStorage extends AbstractComponent implements MetricsStorageService {
    private static final transient Logger LOG = LoggerFactory.getLogger(RhqMetricsStorage.class);

    @Reference
    private Configurer configurer;
    private RHQMetrics metricsService;

    @Property(name = "nodes", label = "Cassandra Nodes", description = "The host names or IP addresses of the cassandra nodes", cardinality = Integer.MAX_VALUE)
    private String[] nodes;

    @Property(name = "keyspace", label = "Cassandra Keyspace", description = "The Cassandra Keyspace (schema)", value = "rhq")
    private String keyspace;

    @Property(name = "cqlport", label = "Cassandra CQL Port", description = "The port number to communicate with a Cassandra host to perform SQL statements", intValue = 9042)
    private int cqlport;

    @Activate
    void activate(Map<String, String> configuration) throws Exception {
        updateConfiguration(configuration);
        activateComponent();
    }

    @Modified
    void modified(Map<String, String> configuration) throws Exception {
        updateConfiguration(configuration);
    }

    @Deactivate
    void deactivate() throws MBeanRegistrationException, InstanceNotFoundException {
        deactivateComponent();
        shutdownMetricService();
    }

    protected void updateConfiguration(Map<String, String> configuration) throws Exception {
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

    protected RHQMetrics createMetricService(Map<String, String> configuration) throws Exception {
        LOG.info("Configuring RHQ metric service from " + configuration);

        configurer.configure(configuration, this);

        RHQMetrics.Builder builder = new RHQMetrics.Builder();

        // enable Cassandra discovery if there are any nodes available
        if (nodes != null && nodes.length > 0) {
            builder = builder.withCassandraDataStore();
            LOG.info("Using Cassandra nodes: " + Arrays.asList(nodes));
            builder.withNodes(nodes);
            if (Strings.isNotBlank(keyspace)) {
                builder.withKeyspace(keyspace);
            }
            if (cqlport > 0) {
                builder.withCQLPort(cqlport);
            }
        } else {
            builder = builder.withInMemoryDataStore();
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
