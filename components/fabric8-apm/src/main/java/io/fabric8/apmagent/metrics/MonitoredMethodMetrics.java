/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.apmagent.metrics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MonitoredMethodMetrics {
    protected final ApmAgentContext apmAgentContext;
    private int monitorSize;
    private final List<MethodMetricsProxy> proxyList = new CopyOnWriteArrayList<>();

    MonitoredMethodMetrics(ApmAgentContext apmAgentContext) {
        this.apmAgentContext = apmAgentContext;
    }

    public int getMonitorSize() {
        return monitorSize;
    }

    public synchronized void setMonitorSize(int monitorSize) {
        this.monitorSize = monitorSize;
        //if we've downsized, remove

        while (proxyList.size() > monitorSize) {
            MethodMetricsProxy methodMetricsProxy = proxyList.get(proxyList.size() - 1);
            proxyList.remove(proxyList.size() - 1);
            apmAgentContext.unregisterMethodMetricsMBean(methodMetricsProxy);
        }
    }

    public void calculateMethodMetrics(List<? extends MethodMetrics> methodMetricsList) {
        if (methodMetricsList.size() < proxyList.size()) {
            setMonitorSize(methodMetricsList.size());
        }
        if (methodMetricsList.size() > proxyList.size() && proxyList.size() < monitorSize) {
            int extra = monitorSize - proxyList.size();

            if (extra > 0) {
                for (int i = 0; i < extra; i++) {
                    proxyList.add(createProxy(proxyList.size()));
                }
            }
        }
        for (int i = 0; i < methodMetricsList.size() && i < proxyList.size(); i++) {
            MethodMetricsProxy methodMetricsProxy = proxyList.get(i);
            if (methodMetricsProxy != null) {
                methodMetricsProxy.setMethodMetrics(methodMetricsList.get(i));
            }
        }
    }

    protected MethodMetricsProxy createProxy(int rank) {
        MethodMetricsProxy result = new MethodMetricsProxy();
        apmAgentContext.registerMethodMetricsMBean(rank, result);
        return result;
    }

    protected void destroy() {
        for (MethodMetricsProxy methodMetricsProxy : proxyList) {
            apmAgentContext.unregisterMethodMetricsMBean(methodMetricsProxy);
        }
        proxyList.clear();
    }
}
