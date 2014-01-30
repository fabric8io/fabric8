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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements a sticky load balancer where a unique client ID String is requested from the
 * {@link org.fusesource.gateway.loadbalancer.ClientRequestFacade} and used to keep track of which
 * service was used last time and to use that if its possible and keep a cache of requests to
 */
public class StickyLoadBalancer<T> implements LoadBalancer<T> {
    private final LoadBalancer<T> firstRequestLoadBalancer;
    private final int maximumCacheSize;
    private final Map<String, T> requestCache;

    public StickyLoadBalancer() {
        this(LoadBalancers.STICKY_LOAD_BALANCER_DEFAULT_CACHE_SIZE);
    }

    public StickyLoadBalancer(int maximumCacheSize) {
        this(maximumCacheSize, new RoundRobinLoadBalancer<T>());
    }

    public StickyLoadBalancer(int maximumCacheSize, LoadBalancer<T> firstRequestLoadBalancer) {
        this.firstRequestLoadBalancer = firstRequestLoadBalancer;
        this.maximumCacheSize = maximumCacheSize;
        this.requestCache = new LinkedHashMap(maximumCacheSize + 1, .75F, true) {
            // This method is called just after a new entry has been added
            public boolean removeEldestEntry(Map.Entry eldest) {
                return size() > StickyLoadBalancer.this.maximumCacheSize;
            }
        };
    }

    @Override
    public String toString() {
        return "StickyLoadBalancer{" +
                "maximumCacheSize=" + maximumCacheSize +
                '}';
    }

    @Override
    public T choose(List<T> services, ClientRequestFacade requestFacade) {
        String clientKey = requestFacade.getClientRequestKey();
        T answer;
        synchronized (requestCache) {
            answer = requestCache.get(clientKey);
            if (answer == null) {
                answer = firstRequestLoadBalancer.choose(services, requestFacade);
                if (answer != null) {
                    requestCache.put(clientKey, answer);
                }
            }
        }
        return answer;
    }

    /**
     * Clears the cache of request client IDs to the bound service
     */
    public void flush() {
        synchronized (requestCache) {
            requestCache.clear();
        }
    }
}
