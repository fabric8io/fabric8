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
package io.fabric8.process.spring.boot.itests.service.invoicing;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {InvoicingRestApiTest.class, InvoicingConfiguration.class})
@IntegrationTest("server.port:0")
@WebAppConfiguration
@EnableAutoConfiguration
public class InvoicingRestApiTest extends Assert {

    RestTemplate rest = new TestRestTemplate();

    @Autowired
    EmbeddedWebApplicationContext tomcat;

    // Tests

    @Test
    public void shouldExposeInvoicingApi() throws InterruptedException {
        // Given
        int port = tomcat.getEmbeddedServletContainer().getPort();
        String baseUri = "http://localhost:" + port;

        // When
        String apiResponse = rest.getForObject(baseUri, String.class);

        // Then
        assertTrue(apiResponse.contains(baseUri + "/invoice"));
    }

}