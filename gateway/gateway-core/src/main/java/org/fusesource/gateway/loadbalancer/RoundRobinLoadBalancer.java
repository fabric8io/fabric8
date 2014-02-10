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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round robbin load balancer
 */
public class RoundRobinLoadBalancer<T> implements LoadBalancer<T> {
    AtomicInteger counter = new AtomicInteger(-1);

    @Override
    public T choose(List<T> things, ClientRequestFacade requestFacade) {
        int size = things.size();
        if (size > 0) {
            int value = counter.incrementAndGet();
            int index = value % size;
            if (index < 0) {
                index = 0;
            } else if (index >= size) {
                index = size - 1;
            }
            return things.get(index);
        }
        return null;
    }
}
