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
package io.fabric8.mq.autoscaler;

public class DestinationLimits {
    private int maxDestinationDepth = 10;
    private int maxProducersPerDestination = 1;
    private int maxConsumersPerDestination = 1;

    public DestinationLimits() {
    }

    public int getMaxConsumersPerDestination() {
        return maxConsumersPerDestination;
    }

    public void setMaxConsumersPerDestination(int maxConsumersPerDestination) {
        this.maxConsumersPerDestination = maxConsumersPerDestination;
    }

    public int getMaxProducersPerDestination() {
        return maxProducersPerDestination;
    }

    public void setMaxProducersPerDestination(int maxProducersPerDestination) {
        this.maxProducersPerDestination = maxProducersPerDestination;
    }

    public int getMaxDestinationDepth() {
        return maxDestinationDepth;
    }

    public void setMaxDestinationDepth(int maxDestinationDepth) {
        this.maxDestinationDepth = maxDestinationDepth;
    }

    public String toString() {
        return "DestinationLimits: maxDestinationDepth=" + getMaxDestinationDepth() + ", producerLimit=" + getMaxProducersPerDestination() + ",consumerLimit=" +
                   getMaxConsumersPerDestination();
    }

}
