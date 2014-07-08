/*
 * Copyright 2005-2014 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.apmagent.metrics;

import com.codahale.metrics.Timer;

import java.util.concurrent.TimeUnit;

public class MethodMetrics implements MethodMetricsMBean {
    protected final Timer timer;
    private final String name;
    private final double rateFactor;
    private final double durationFactor;

    /**
     * Constructor.
     *
     * @param name - the fully qualified method name
     */
    public MethodMetrics(String name) {
        this.name = name;
        this.timer = new Timer();

        this.rateFactor = TimeUnit.SECONDS.toSeconds(1);
        this.durationFactor = 1.0 / TimeUnit.MILLISECONDS.toNanos(1);
    }

    public String getName() {
        return name;
    }

    public long getCount() {
        return timer.getCount();
    }

    public double getMeanRate() {
        return timer.getMeanRate() * rateFactor;
    }

    public double getOneMinuteRate() {
        return timer.getOneMinuteRate() * rateFactor;
    }

    public double getFiveMinuteRate() {
        return timer.getFiveMinuteRate() * rateFactor;
    }

    public double getFifteenMinuteRate() {
        return timer.getFifteenMinuteRate() * rateFactor;
    }

    @Override
    public double getMin() {
        return timer.getSnapshot().getMin() * durationFactor;
    }

    @Override
    public double getMax() {
        return timer.getSnapshot().getMax() * durationFactor;
    }

    @Override
    public double getMean() {
        return timer.getSnapshot().getMean() * durationFactor;
    }

    @Override
    public double getStdDev() {
        return timer.getSnapshot().getStdDev() * durationFactor;
    }

    @Override
    public double get50thPercentile() {
        return timer.getSnapshot().getMedian() * durationFactor;
    }

    @Override
    public double get75thPercentile() {
        return timer.getSnapshot().get75thPercentile() * durationFactor;
    }

    @Override
    public double get95thPercentile() {
        return timer.getSnapshot().get95thPercentile() * durationFactor;
    }

    @Override
    public double get98thPercentile() {
        return timer.getSnapshot().get98thPercentile() * durationFactor;
    }

    @Override
    public double get99thPercentile() {
        return timer.getSnapshot().get99thPercentile() * durationFactor;
    }

    @Override
    public double get999thPercentile() {
        return timer.getSnapshot().get999thPercentile() * durationFactor;
    }

    @Override
    public long[] values() {
        return timer.getSnapshot().getValues();
    }


    public void update(long elapsed) {
        if (elapsed >= 0) {
            timer.update(elapsed, TimeUnit.NANOSECONDS);
        }
    }

    public String toString() {
        return "MethodMetrics:" + getName();
    }

}

