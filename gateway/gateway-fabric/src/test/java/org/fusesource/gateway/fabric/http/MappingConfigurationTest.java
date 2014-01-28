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
package org.fusesource.gateway.fabric.http;

import org.fusesource.gateway.handlers.http.HttpGateway;
import org.fusesource.gateway.handlers.http.MappedServices;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class MappingConfigurationTest {
    protected HttpGateway httpGateway = new FabricHTTPGateway();
    protected HttpMappingRuleConfiguration config = new HttpMappingRuleConfiguration();


    @Test
    public void testContextPath() throws Exception {
        config.setUriTemplate("{contextPath}/");

        addQuickstartServices();

        assertMapping("/cxf/HelloWorld/", "http://localhost:8183/cxf/HelloWorld");
        assertMapping("/cxf/crm/", "http://localhost:8182/cxf/crm");
    }

    @Test
    public void testPrefixAndContextPath() throws Exception {
        config.setUriTemplate("/foo{contextPath}/");

        addQuickstartServices();

        assertMapping("/foo/cxf/HelloWorld/", "http://localhost:8183/cxf/HelloWorld");
        assertMapping("/foo/cxf/crm/", "http://localhost:8182/cxf/crm");
    }

    @Test
    public void testPrefixVersionAndContextPath() throws Exception {
        config.setUriTemplate("/bar/{version}{contextPath}/");

        addQuickstartServices();

        assertMapping("/bar/1.0/cxf/HelloWorld/", "http://localhost:8183/cxf/HelloWorld");
        assertMapping("/bar/1.0/cxf/crm/", "http://localhost:8182/cxf/crm");
    }

    protected void addService(String path, String service) {
        config.updateMappingRules(false, path, Arrays.asList(service));
    }

    protected void assertMapping(String path, String service) {
        Map<String, MappedServices> mappingRules = httpGateway.getMappedServices();
        assertTrue("Should have some mapping rules", mappingRules.size() > 0);

        MappedServices mappedServices = mappingRules.get(path);
        assertNotNull("Could not find mapping rule for path " + path, mappedServices);

        Set<String> serviceUrls = mappedServices.getServiceUrls();
        assertTrue("Could not find service " + service + " in services " + serviceUrls, serviceUrls.contains(service));
    }

    protected void printMappings(Map<String, MappedServices> mappingRules) {
        for (Map.Entry<String, MappedServices> entry : mappingRules.entrySet()) {
            String key = entry.getKey();
            MappedServices value = entry.getValue();
            System.out.println(key + " => " + value.getServiceUrls());
        }
    }

    protected void addQuickstartServices() {
        httpGateway.addMappingRuleConfiguration(config);

        addService("rest/CustomerService/crm/1.0/resty", "http://localhost:8182/cxf/crm");
        addService("ws/HelloWorldImplPort/HelloWorld/1.0/soapy", "http://localhost:8183/cxf/HelloWorld");

        Map<String, MappedServices> mappingRules = httpGateway.getMappedServices();
        printMappings(mappingRules);
    }


}
