/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.insight.influxdb;

import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.insight.metrics.model.MBeanAttrResult;
import io.fabric8.insight.metrics.model.MBeanAttrsResult;
import io.fabric8.insight.metrics.model.MBeanOperResult;
import io.fabric8.insight.metrics.model.MBeanOpersResult;
import io.fabric8.insight.metrics.model.Metrics;
import io.fabric8.insight.metrics.model.MetricsStorageService;
import io.fabric8.insight.metrics.model.QueryResult;
import io.fabric8.insight.metrics.model.Result;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@ThreadSafe
@Component(name = "io.fabric8.insight.influxdb.metrics", label = "Fabric8 InfluxDB Metrics Storage", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service(MetricsStorageService.class)
@Properties(
        @Property(name = Constants.SERVICE_RANKING, intValue = 1)
)
public class InfluxDBMetricsStorage extends AbstractComponent implements MetricsStorageService {
    private static final transient Logger LOG = LoggerFactory.getLogger(InfluxDBMetricsStorage.class);

    @Reference(referenceInterface = InfluxDB.class)
    private ValidatingReference<InfluxDB> influxDB = new ValidatingReference<>();

    @Activate
    void activate() throws Exception {
        activateComponent();
    }


    @Deactivate
    void deactivate() throws MBeanRegistrationException, InstanceNotFoundException {
        deactivateComponent();
    }

    @Override
    public void store(String type, long timestamp, QueryResult queryResult) {
        assertValid();
        if (influxDB == null) {
            throw new IllegalStateException("No influxDB available!");
        }
        List<Serie> series = new LinkedList<>();

        Map<String, Result<?>> results = queryResult.getResults();
        if (results != null) {
            Map<String, Object> data = new HashMap<>();

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
                                data.put(id, doubleValue);
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
                                        data.put(id, doubleValue);
                                    }
                                }
                            }
                        }
                    }
                }

                if (!data.isEmpty()) {
                    data.put("time", timestamp);
                    series.add(new Serie.Builder("insight")
                                    .columns(data.keySet().toArray(new String[data.size()]))
                                    .values(data.values().toArray(new Object[data.size()]))
                                    .build()

                    );
                }
            }
            if (!series.isEmpty()) {
                influxDB.get().write("fabric", TimeUnit.MILLISECONDS, series.toArray(new Serie[series.size()]));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("added " + series.size() + " metrics");
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

    void bindInfluxDB(InfluxDB service) {
        influxDB.bind(service);
    }

    void unbindInfluxDB(InfluxDB service) {
        influxDB.unbind(service);
    }

}
