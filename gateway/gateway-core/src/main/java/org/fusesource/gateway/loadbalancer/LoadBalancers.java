/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.gateway.loadbalancer;

import org.fusesource.common.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods for using and creating load balancers
 */
public class LoadBalancers {
    private static final transient Logger LOG = LoggerFactory.getLogger(LoadBalancers.class);

    public static final String RANDOM_LOAD_BALANCER = "random";
    public static final String ROUND_ROBIN_LOAD_BALANCER = "roundrobin";
    public static final String STICKY_LOAD_BALANCER = "sticky";

    public static final int STICKY_LOAD_BALANCER_DEFAULT_CACHE_SIZE = 10000;

    public static <T> LoadBalancer<T> createLoadBalancer(String loadBalancerType, int stickyLoadBalancerCacheSize) {
        if (RANDOM_LOAD_BALANCER.equals(loadBalancerType)) {
            return new RandomLoadBalancer<T>();
        } else if (ROUND_ROBIN_LOAD_BALANCER.equals(loadBalancerType)) {
            return new RoundRobinLoadBalancer<T>();
        } else if (STICKY_LOAD_BALANCER.equals(loadBalancerType)) {
            return new StickyLoadBalancer<T>(stickyLoadBalancerCacheSize);
        } else {
            if (Strings.isNotBlank(loadBalancerType)) {
                LOG.warn("Ignored invalid load balancer type: " + loadBalancerType);
            }

            // lets use round robin as sensible default
            return new RoundRobinLoadBalancer<T>();
        }
    }
}
