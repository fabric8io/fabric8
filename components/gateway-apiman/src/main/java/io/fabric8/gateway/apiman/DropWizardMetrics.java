/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.gateway.apiman;

import io.apiman.gateway.engine.IComponentRegistry;
import io.apiman.gateway.engine.IMetrics;
import io.apiman.gateway.engine.metrics.RequestMetric;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;

/**
 * A drop-wizard implementation of the apiman metrics interface.
 *
 * @author eric.wittmann@redhat.com
 */
public class DropWizardMetrics implements IMetrics {

    private final MetricRegistry registry = new MetricRegistry();

    /**
     * Constructor.
     */
    public DropWizardMetrics() {
        final JmxReporter reporter = JmxReporter.forRegistry(registry).build();
        reporter.start();
    }

    /**
     * @see io.apiman.gateway.engine.IMetrics#record(io.apiman.gateway.engine.metrics.RequestMetric)
     */
    @Override
    public void record(RequestMetric metric) {
        registry.meter(name("ServiceRequest", metric)).mark();
        registry.timer(name("ServiceRequestDuration", metric)).update(metric.getRequestDuration(), TimeUnit.MILLISECONDS);
        registry.timer(name("ServiceDuration", metric)).update(metric.getServiceDuration(), TimeUnit.MILLISECONDS);
        if (metric.isError()) {
            registry.meter(name("ServiceRequestError", metric)).mark();
        } else if (metric.getFailureCode() > 0) {
            registry.meter(name("ServiceRequestFailure", metric)).mark();
        } else {
            registry.meter(name("ServiceRequestSuccess", metric)).mark();
        }
    }

    /**
     * @param metricType
     * @param metric
     */
    private String name(String metricType, RequestMetric metric) {
        return MetricRegistry.name(metricType, metric.getServiceOrgId(), metric.getServiceId(), metric.getServiceVersion());
    }

    /**
     * @see io.apiman.gateway.engine.IMetrics#setComponentRegistry(io.apiman.gateway.engine.IComponentRegistry)
     */
    @Override
    public void setComponentRegistry(IComponentRegistry registry) {
        // Not required by this implementation.
    }

}
