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

import com.codahale.metrics.Timer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MethodMetrics {
    protected final Timer timer;
    private final String name;
    private final double rateFactor;
    private final double durationFactor;
    private int percentage;
    private boolean active = true;

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public double getMin() {
        return timer.getSnapshot().getMin() * durationFactor;
    }

    public double getMax() {
        return timer.getSnapshot().getMax() * durationFactor;
    }

    public double getMean() {
        return timer.getSnapshot().getMean() * durationFactor;
    }

    public double getStdDev() {
        return timer.getSnapshot().getStdDev() * durationFactor;
    }

    public double get50thPercentile() {
        return timer.getSnapshot().getMedian() * durationFactor;
    }

    public double get75thPercentile() {
        return timer.getSnapshot().get75thPercentile() * durationFactor;
    }

    public double get95thPercentile() {
        return timer.getSnapshot().get95thPercentile() * durationFactor;
    }

    public double get98thPercentile() {
        return timer.getSnapshot().get98thPercentile() * durationFactor;
    }

    public double get99thPercentile() {
        return timer.getSnapshot().get99thPercentile() * durationFactor;
    }

    public double get999thPercentile() {
        return timer.getSnapshot().get999thPercentile() * durationFactor;
    }

    /**
     * average amount of time for a method multiplied by the number of times called
     *
     * @return estimated load
     */
    public double getLoad() {
        return timer.getSnapshot().size() * getMean();
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

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

    public static List<? extends MethodMetrics> sortedMetrics(Collection<? extends MethodMetrics> collection) {
        ArrayList<? extends MethodMetrics> list = new ArrayList<>(collection);

        Collections.sort(list, new Comparator<MethodMetrics>() {
            @Override
            public int compare(MethodMetrics methodMetrics1, MethodMetrics methodMetrics2) {
                return (int) ((int) methodMetrics2.getLoad() - (int) methodMetrics1.getLoad());
            }
        });
        //calculate the percentage
        int totalLoad = 0;
        for (MethodMetrics m : list) {
            totalLoad += m.getLoad();
        }
        for (MethodMetrics m : list) {
            int percentage = (int) ((m.getLoad() * 100) / totalLoad);
            m.setPercentage(percentage);
        }
        return list;
    }
}

