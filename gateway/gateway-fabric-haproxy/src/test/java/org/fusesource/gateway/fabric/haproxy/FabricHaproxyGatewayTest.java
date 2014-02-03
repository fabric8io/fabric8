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
package org.fusesource.gateway.fabric.haproxy;

import io.fabric8.utils.Files;
import io.fabric8.zookeeper.internal.SimplePathTemplate;
import org.fusesource.gateway.ServiceDTO;
import org.fusesource.gateway.fabric.support.http.HttpMappingRuleBase;
import org.fusesource.gateway.loadbalancer.LoadBalancer;
import org.fusesource.gateway.loadbalancer.RoundRobinLoadBalancer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class FabricHaproxyGatewayTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricHaproxyGatewayTest.class);

    @Rule
    public TestName testName = new TestName();

    protected FabricHaproxyGateway gateway = new FabricHaproxyGateway();
    protected HttpMappingRuleBase config;
    private String oldVersion = "1.0";
    private String newVersion = "1.1";
    private String enabledVersion = null;
    private LoadBalancer<String> loadBalancer = new RoundRobinLoadBalancer<String>();
    private boolean reverseHeaders = true;
    private File outputFile;

    @Before
    public void init() throws Exception {
        String basedir = System.getProperty("basedir", ".");
        outputFile = new File(basedir + "/target/test-data/haproxy-" + testName.getMethodName() + ".cfg");
        outputFile.getParentFile().mkdirs();

        String reloadCommand = "cat " + outputFile.getAbsolutePath();
        gateway.setReloadCommand(reloadCommand);

        gateway.setConfigFile(outputFile.getAbsolutePath());
        String name = "config.mvel";
        URL resource = getClass().getResource(name);
        if (resource == null) {
            resource = getClass().getClassLoader().getResource("org/fusesource/gateway/haproxy/config.mvel");
        }
        assertNotNull("Should have found config file " + name + " on the classpath", resource);
        InputStream inputStream = resource.openStream();
        assertNotNull("Could not open the stream for " + resource, inputStream);
        String templateText = Files.toString(inputStream);
        assertNotNull("Should have loaded a the template from " + name);
        gateway.setTemplateText(templateText);
    }

    @Test
    public void testGenerateTemplate() throws Exception {
        setUriTemplate("/bar/{version}{contextPath}/", oldVersion);

        addQuickstartServices();

        // now lets load the generated file
        assertTrue("Should have generated " + outputFile, outputFile.exists() && outputFile.isFile());
        List<String> lines = Files.readLines(outputFile);
        assertLinesContains(lines,
                "use_backend b_bar_1.0_cxf_crm if { path_beg /bar/1.0/cxf/crm/ }",
                "use_backend b_bar_1.0_cxf_HelloWorld if { path_beg /bar/1.0/cxf/HelloWorld/ }",

                "backend b_bar_1.0_cxf_crm",
                "server resty  localhost:8182",

                "backend b_bar_1.0_cxf_HelloWorld",
                "server soapy  localhost:8183"
        );

        LOG.info("About to reload the proxy command");
        gateway.reloadHaproxy();

        Thread.sleep(3000);
        LOG.info("Done!");
    }

    protected void assertLinesContains(List<String> lines, String... expectedLines) {
        for (String expectedLine : expectedLines) {
            boolean found = false;
            for (String line : lines) {
                if (line != null) {
                    String trimmed = line.trim();
                    if (trimmed.equals(expectedLine)) {
                        found = true;
                        break;
                    }
                }
            }
            assertTrue("Did not find expected '" + expectedLine + "' in generated file " + outputFile, found);
        }
    }

    protected void setUriTemplate(String uriTemplate, String version) {
        config = new HttpMappingRuleBase(
                new SimplePathTemplate(uriTemplate), version, enabledVersion, loadBalancer, reverseHeaders);
        gateway.addMappingRuleConfiguration(config);
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

    protected void addQuickstartServices() {
        addService("rest/CustomerService/crm/1.0/resty", "http://localhost:8182/cxf/crm", oldVersion);
        addService("ws/HelloWorldImplPort/HelloWorld/1.0/soapy", "http://localhost:8183/cxf/HelloWorld", oldVersion);
    }
}
