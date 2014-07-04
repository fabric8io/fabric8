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
package io.fabric8.process.spring.boot.data;

import io.fabric8.process.spring.boot.data.domain.Invoice;
import io.fabric8.process.spring.boot.data.repository.InvoiceQuery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.UUID.randomUUID;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {InvoicingRestApiTest.class, InvoicingConfiguration.class})
@IntegrationTest("server.port:0")
@WebAppConfiguration
@EnableAutoConfiguration
@ComponentScan
public class InvoicingRestApiTest extends Assert {

    // Test subject fixture

    RestRepository<Invoice, Long> restRepository;

    // Collaborators fixtures

    @Autowired
    EmbeddedWebApplicationContext tomcat;

    int port;

    String baseUri;

    // Data fixtures

    Invoice invoice;

    // Fixtures setup

    @Before
    public void before() throws MalformedURLException {
        port = tomcat.getEmbeddedServletContainer().getPort();
        baseUri = "http://localhost:" + port + "/invoice";

        restRepository = new TemplateRestRepository<>(Invoice.class, baseUri);

        invoice = new Invoice().invoiceId(randomUUID().toString());
        invoice = restRepository.save(invoice);
    }

    // Tests

    @Test
    public void shouldFindInvoiceById() throws InterruptedException {
        // When
        Invoice loadedInvoice = restRepository.findOne(invoice.id());

        // Then
        assertEquals(invoice.id(), loadedInvoice.id());
    }

    @Test
    public void shouldSaveInvoices() throws InterruptedException {
        // Given
        Invoice firstInvoice = new Invoice().invoiceId(randomUUID().toString());
        Invoice secondInvoice = new Invoice().invoiceId(randomUUID().toString());

        // When
        List<Invoice> savedInvoices = newArrayList(restRepository.save(Arrays.asList(firstInvoice, secondInvoice)));

        // Then
        assertEquals(firstInvoice.id(), savedInvoices.get(0).id());
        assertEquals(secondInvoice.id(), savedInvoices.get(1).id());
    }

    @Test
    public void shouldCountInvoicesByQuery() throws InterruptedException {
        // When
        long count = restRepository.countByQuery(new InvoiceQuery().invoiceId(invoice.invoiceId()));

        // Then
        assertEquals(1, count);
    }

    @Test
    public void shouldFindInvoicesByQuery() throws InterruptedException {
        // Given
        InvoiceQuery query = new InvoiceQuery().invoiceId(invoice.invoiceId());

        // When
        Iterable<Invoice> receivedInvoices = restRepository.findByQuery(query);

        // Then
        assertEquals(invoice.id(), receivedInvoices.iterator().next().id());
    }

}