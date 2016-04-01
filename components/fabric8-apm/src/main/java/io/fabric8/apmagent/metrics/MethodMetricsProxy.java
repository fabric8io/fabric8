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

public class MethodMetricsProxy implements MethodMetricsProxyMBean {
    private MethodMetrics methodMetrics;

    void setMethodMetrics(MethodMetrics methodMetrics) {
        this.methodMetrics = methodMetrics;
    }

    @Override
    public String getName() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.getName() : "";
    }

    @Override
    public long getCount() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.getCount() : 0;
    }

    @Override
    public double getMeanRate() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.getMeanRate() : 0;
    }

    @Override
    public double getOneMinuteRate() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.getOneMinuteRate() : 0;
    }

    @Override
    public double getFiveMinuteRate() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.getFiveMinuteRate() : 0;
    }

    @Override
    public double getFifteenMinuteRate() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.getFifteenMinuteRate() : 0;
    }

    @Override
    public double getMin() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.getMin() : 0;
    }

    @Override
    public double getMax() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.getMax() : 0;
    }

    @Override
    public double getMean() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.getMean() : 0;
    }

    @Override
    public double getStdDev() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.getStdDev() : 0;
    }

    @Override
    public double get50thPercentile() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.get50thPercentile() : 0;
    }

    @Override
    public double get75thPercentile() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.get75thPercentile() : 0;
    }

    @Override
    public double get95thPercentile() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.get95thPercentile() : 0;
    }

    @Override
    public double get98thPercentile() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.get98thPercentile() : 0;
    }

    @Override
    public double get99thPercentile() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.get99thPercentile() : 0;
    }

    @Override
    public double get999thPercentile() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.get999thPercentile() : 0;
    }

    @Override
    public long[] values() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.values() : new long[0];
    }

    @Override
    public double getLoad() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.getLoad() : 0;
    }

    @Override
    public int getPercentage() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.getPercentage() : 0;
    }

    @Override
    public String toString() {
        MethodMetrics mm = this.methodMetrics;
        return mm != null ? mm.toString() : "EmptyMethodMetricsProxy";
    }
}
