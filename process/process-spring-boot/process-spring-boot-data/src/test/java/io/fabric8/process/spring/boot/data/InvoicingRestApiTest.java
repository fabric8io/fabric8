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
import io.fabric8.process.spring.boot.data.domain.InvoiceListingRecord;
import io.fabric8.process.spring.boot.data.domain.InvoiceQuery;
import io.fabric8.process.spring.boot.registry.ProcessRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.fabric8.process.spring.boot.data.TemplateRestRepository.forRegistrySymbol;
import static java.util.UUID.randomUUID;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {InvoicingRestApiTest.class, InvoicingConfiguration.class})
@IntegrationTest("server.port:6667")
@WebAppConfiguration
@EnableAutoConfiguration
@ComponentScan
public class InvoicingRestApiTest extends Assert {

    // Test subject fixture

    RestRepository<Invoice, Long> restRepository;

    // Data fixtures

    Invoice invoice;

    // Fixtures setup

    @Autowired
    ProcessRegistry processRegistry;

    @Before
    public void before() throws MalformedURLException {

        restRepository = forRegistrySymbol(processRegistry, "invoicing", Invoice.class);

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
    public void shouldUpdateInvoice() throws InterruptedException {
        // Given
        invoice.invoiceId("newId");
        restRepository.save(invoice);

        // When
        Invoice loadedInvoice = restRepository.findOne(invoice.id());

        // Then
        assertEquals("newId", loadedInvoice.invoiceId());
    }

    @Test
    public void shouldSaveInvoices() throws InterruptedException {
        // Given
        Invoice firstInvoice = new Invoice().invoiceId(randomUUID().toString());
        Invoice secondInvoice = new Invoice().invoiceId(randomUUID().toString());

        // When
        List<Invoice> savedInvoices = newArrayList(restRepository.save(Arrays.asList(firstInvoice, secondInvoice)));

        // Then
        assertEquals(firstInvoice.invoiceId(), savedInvoices.get(0).invoiceId());
        assertEquals(secondInvoice.invoiceId(), savedInvoices.get(1).invoiceId());
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

    @Test
    public void shouldListInvoicesRecordsByQuery() throws InterruptedException {
        // Given
        InvoiceQuery query = new InvoiceQuery().invoiceId(invoice.invoiceId());

        // When
        Iterable<InvoiceListingRecord> receivedInvoices = restRepository.listByQuery(query, InvoiceListingRecord.class);

        // Then
        assertEquals(invoice.id(), receivedInvoices.iterator().next().id());
        assertNotNull(receivedInvoices.iterator().next().getListingLabel());
    }


}
