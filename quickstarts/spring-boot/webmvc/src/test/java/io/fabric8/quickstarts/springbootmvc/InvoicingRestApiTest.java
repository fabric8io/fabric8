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
package io.fabric8.quickstarts.springbootmvc;

import io.fabric8.quickstarts.springbootmvc.domain.Invoice;
import io.fabric8.quickstarts.springbootmvc.repository.InvoiceRepository;
import org.junit.Assert;
import org.junit.Before;
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

    int port;

    String baseUri;

    @Autowired
    InvoiceRepository invoiceRepository;

    @Before
    public void before() {
        port = tomcat.getEmbeddedServletContainer().getPort();
        baseUri = "http://localhost:" + port;
    }

    // Tests

    @Test
    public void shouldExposeInvoicingApi() throws InterruptedException {
        // When
        String apiResponse = rest.getForObject(baseUri, String.class);

        // Then
        assertTrue(apiResponse.contains(baseUri + "/invoice"));
    }

    @Test
    public void shouldReadInvoice() throws InterruptedException {
        // Given
        Invoice invoice = new Invoice().invoiceId("INV-2014-01-01");
        invoice = invoiceRepository.save(invoice);

        String invoiceUri = baseUri + "/invoice/" + invoice.id();

        // When
        Invoice receivedInvoice = rest.getForObject(invoiceUri, Invoice.class);

        // Then
        assertEquals(invoice.invoiceId(), receivedInvoice.invoiceId());
    }

}