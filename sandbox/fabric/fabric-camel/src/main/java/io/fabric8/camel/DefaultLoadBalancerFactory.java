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
package io.fabric8.camel;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.processor.loadbalancer.LoadBalancer;
import org.apache.camel.processor.loadbalancer.RandomLoadBalancer;

import java.util.List;

public class DefaultLoadBalancerFactory implements LoadBalancerFactory {

    public LoadBalancer createLoadBalancer() {
        return new RandomLoadBalancer() {
            @Override
            public boolean process(Exchange exchange, AsyncCallback callback) {
                List<Processor> list = getProcessors();
                if (!list.isEmpty()) {
                    return super.process(exchange, callback);
                } else {
                    throw new IllegalStateException("No processors found.");
                }
            }
        };
    }
}
