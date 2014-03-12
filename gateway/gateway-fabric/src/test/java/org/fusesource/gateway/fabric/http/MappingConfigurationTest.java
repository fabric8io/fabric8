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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.fabric8.zookeeper.internal.SimplePathTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.fusesource.gateway.ServiceDTO;
import org.fusesource.gateway.fabric.support.http.HttpMappingRuleBase;
import org.fusesource.gateway.handlers.http.MappedServices;
import org.fusesource.gateway.loadbalancer.LoadBalancer;
import org.fusesource.gateway.loadbalancer.RoundRobinLoadBalancer;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class MappingConfigurationTest {

    protected FabricHTTPGateway httpGateway = new FabricHTTPGateway();
    protected HttpMappingRuleBase config;
    private String oldVersion = "1.0";
    private String newVersion = "1.1";
    private String enabledVersion = null;
    private LoadBalancer<String> loadBalancer = new RoundRobinLoadBalancer<String>();
    private boolean reverseHeaders = true;

    @Before
    public void setUp() {
        httpGateway.activateComponent();
    }

    @Test
    public void testContextPath() throws Exception {
        setUriTemplate("{contextPath}/", oldVersion);

        addQuickstartServices();

        assertMapping("/cxf/HelloWorld/", "http://localhost:8183/cxf/HelloWorld");
        assertMapping("/cxf/crm/", "http://localhost:8182/cxf/crm");
    }

    @Test
    public void testPrefixAndContextPath() throws Exception {
        setUriTemplate("/foo{contextPath}/", oldVersion);

        addQuickstartServices();

        assertMapping("/foo/cxf/HelloWorld/", "http://localhost:8183/cxf/HelloWorld");
        assertMapping("/foo/cxf/crm/", "http://localhost:8182/cxf/crm");
    }

    @Test
    public void testPrefixVersionAndContextPath() throws Exception {
        setUriTemplate("/bar/{version}{contextPath}/", oldVersion);

        addQuickstartServices();

        assertMapping("/bar/1.0/cxf/HelloWorld/", "http://localhost:8183/cxf/HelloWorld");
        assertMapping("/bar/1.0/cxf/crm/", "http://localhost:8182/cxf/crm");
    }

    @Test
    public void testHideNewVersions() throws Exception {
        setUriTemplate("{contextPath}/", oldVersion);

        addNewQuickstartServices();
        addQuickstartServices();

        assertMapping("/cxf/HelloWorld/", "http://localhost:8183/cxf/HelloWorld");
        assertMapping("/cxf/crm/", "http://localhost:8182/cxf/crm");

        assertEquals("mapping size",  2, httpGateway.getMappedServices().size());
    }

    @Test
    public void testHideOldVersions() throws Exception {
        setUriTemplate("{contextPath}/", newVersion);

        addNewQuickstartServices();
        addQuickstartServices();

        assertMapping("/cxf/HelloWorld/", "http://localhost:8185/cxf/HelloWorld");
        assertMapping("/cxf/crm/", "http://localhost:8184/cxf/crm");

        assertEquals("mapping size",  2, httpGateway.getMappedServices().size());
    }

    protected void setUriTemplate(String uriTemplate, String version) {
        config = new HttpMappingRuleBase(
                new SimplePathTemplate(uriTemplate), version, enabledVersion, loadBalancer, reverseHeaders);
        httpGateway.addMappingRuleConfiguration(config);
    }

    protected void addService(String path, String service, String version) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("version", version);
        String container = path.contains("HelloWorld") ? "soapy" : "resty";
        params.put("container", container);
        ServiceDTO serviceDetails = new ServiceDTO();
        serviceDetails.setContainer(container);
        serviceDetails.setVersion(version);
        config.updateMappingRules(false, path, Arrays.asList(service), params, serviceDetails);
    }

    protected void assertMapping(String path, String service) {
        Map<String, MappedServices> mappingRules = httpGateway.getMappedServices();
        assertTrue("Should have some mapping rules", mappingRules.size() > 0);

        MappedServices mappedServices = mappingRules.get(path);
        assertNotNull("Could not find mapping rule for path " + path, mappedServices);

        Collection<String> serviceUrls = mappedServices.getServiceUrls();
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

        addService("rest/CustomerService/crm/1.0/resty", "http://localhost:8182/cxf/crm", oldVersion);
        addService("ws/HelloWorldImplPort/HelloWorld/1.0/soapy", "http://localhost:8183/cxf/HelloWorld", oldVersion);

        Map<String, MappedServices> mappingRules = httpGateway.getMappedServices();
        printMappings(mappingRules);
    }

    protected void addNewQuickstartServices() {
        addService("rest/CustomerService/crm/1.1/resty2", "http://localhost:8184/cxf/crm", newVersion);
        addService("ws/HelloWorldImplPort/HelloWorld/1.1/soapy2", "http://localhost:8185/cxf/HelloWorld", newVersion);
    }


}
