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
package io.fabric8.gateway.model.loadbalancer;

import io.fabric8.gateway.loadbalancer.LoadBalancer;

import java.util.concurrent.atomic.AtomicReference;

/**
 */
public abstract class LoadBalancerDefinition<T extends LoadBalancer> {
    private AtomicReference<T> loadBalancerReference = new AtomicReference<T>(null);

    /**
     * Returns the lazily created load balancer
     */
    public T getLoadBalancer() {
        T answer = loadBalancerReference.get();
        if (answer == null) {
            loadBalancerReference.compareAndSet(null, createLoadBalancer());
            answer = loadBalancerReference.get();
        }
        return answer;
    }

    /**
     * Factory method to create a load balancer instance from its configuration
     */
    protected abstract T createLoadBalancer();
}
