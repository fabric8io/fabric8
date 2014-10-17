/*
 *
 *  * Copyright 2005-2014 Red Hat, Inc.
 *  * Red Hat licenses this file to you under the Apache License, version
 *  * 2.0 (the "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  * implied.  See the License for the specific language governing
 *  * permissions and limitations under the License.
 *
 */

package io.fabric8.mq.autoscaler;

import static io.fabric8.mq.autoscaler.EnvUtils.getEnv;

public class DestinationLimits {
    private int depthLimit = 10;
    private int producersLimit = 1;
    private int consumersLimit = 1;

    public DestinationLimits() {
        getEnv("depthLimit", 10);
        getEnv("producersLimit", 2);
        getEnv("consumersLimit", 2);
    }

    public int getConsumersLimit() {
        return consumersLimit;
    }

    public void setConsumersLimit(int consumersLimit) {
        this.consumersLimit = consumersLimit;
    }

    public int getProducersLimit() {
        return producersLimit;
    }

    public void setProducersLimit(int producersLimit) {
        this.producersLimit = producersLimit;
    }

    public int getDepthLimit() {
        return depthLimit;
    }

    public void setDepthLimit(int depthLimit) {
        this.depthLimit = depthLimit;
    }

    public String toString() {
        return "DestinationLimits: depthLimit=" + getDepthLimit() + ", producerLimit=" + getProducersLimit() + ",consumerLimit=" +
                   getConsumersLimit();
    }

}
