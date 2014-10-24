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
package io.fabric8.gateway.loadbalancer;

import java.util.List;

/**
 * Random load balancer
 */
public class RandomLoadBalancer implements LoadBalancer {

    @Override
    public <T> T choose(List<T> things, ClientRequestFacade requestFacade) {
        int size = things.size();
        if (size == 1) {
            return things.get(0);
        } else if (size > 1) {
            int idx = (int) Math.round(Math.random() * (size - 1));
            if (idx < 0) {
                idx = 0;
            }
            if (idx >= size) {
                idx = size - 1;
            }
            return things.get(idx);
        }
        return null;
    }
}
