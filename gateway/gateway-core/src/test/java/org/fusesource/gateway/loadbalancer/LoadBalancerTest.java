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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class LoadBalancerTest {

    protected String clientRequestKey = "client1";

    protected ClientRequestFacade clientRequestFacade = new ClientRequestFacade() {
        @Override
        public String getClientRequestKey() {
            return clientRequestKey;
        }
    };

    protected List<String> services = Arrays.asList(
        "http://localhost:8182/foo",
        "http://localhost:8183/foo",
        "http://localhost:8184/foo",
        "http://localhost:8185/foo"
    );

    protected int requestCount = services.size() * 2;

    @Test
    public void testRandomLoadBalancer() throws Exception {
        LoadBalancer<String> loadBalancer = new RandomLoadBalancer<String>();
        assertLoadBalancerWorksOnEmptyOrSingletonServices(loadBalancer);
        List<String> results = performRequests(loadBalancer);
        Set<String> set = asSet(results);
        assertTrue("Should have most of the values but was: " + set, set.size() > 1);
    }

    protected Set<String> asSet(List<String> results) {
        Set<String> set = new HashSet<String>();
        set.addAll(results);
        return set;
    }

    @Test
    public void testRoundRobbinLoadBalancer() throws Exception {
        LoadBalancer<String> loadBalancer = new RoundRobinLoadBalancer<String>();
        assertLoadBalancerWorksOnEmptyOrSingletonServices(loadBalancer);
        List<String> results = performRequests(loadBalancer);
        Set<String> set = asSet(results);
        assertEquals("Should have all of the values: " + set, services.size(), set.size());
        for (String service : services) {
            assertTrue("Should have found service: " + service, set.contains(service));
        }
    }

    @Test
    public void testStickyLoadBalancer() throws Exception {
        assertLoadBalancerWorksOnEmptyOrSingletonServices(new StickyLoadBalancer<String>());

        LoadBalancer<String> loadBalancer = new StickyLoadBalancer<String>();
        Set<String> allRequests = new HashSet<String>();
        int numberOfClients = 10;
        for (int i = 0; i < numberOfClients; i++) {
            clientRequestKey = "newClient:" + i;

            List<String> results = performRequests(loadBalancer);
            Set<String> set = asSet(results);
            assertTrue("All values should be the same for client: " + clientRequestKey + " but got: " + set, set.size() == 1);
            allRequests.addAll(set);
        }

        // now we should have a reasonable number of different overall answers.
        assertTrue("Across " + numberOfClients + " we should have most of the values: " + allRequests, allRequests.size() > 1);

    }

    protected List<String> performRequests(LoadBalancer<String> loadBalancer) {
        List<String> answer = new ArrayList<String>();
        for (int i = 0; i < requestCount; i++) {
            String result = loadBalancer.choose(services, clientRequestFacade);
            assertNotNull("No service found for load balancer " + loadBalancer + " on request #" + i, result);
            answer.add(result);
        }
        assertEquals("number of results", requestCount, answer.size());
        return answer;
    }

    protected void assertLoadBalancerWorksOnEmptyOrSingletonServices(LoadBalancer<String> loadBalancer) {
        // lets test empty list
        String emptyResult = loadBalancer.choose(Collections.EMPTY_LIST, clientRequestFacade);
        assertEquals("Should not find any results!", null, emptyResult);

        // lets test list with single result
        String expectedSingle = "http://singleton.acme.com/";
        String actualSingle = loadBalancer.choose(Arrays.asList(expectedSingle), clientRequestFacade);
        assertEquals("Using single list", expectedSingle, actualSingle);
    }
}
