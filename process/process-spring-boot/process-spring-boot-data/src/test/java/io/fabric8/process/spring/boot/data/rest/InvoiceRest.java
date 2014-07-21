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
package io.fabric8.process.spring.boot.data.rest;

import io.fabric8.process.spring.boot.data.domain.Invoice;
import io.fabric8.process.spring.boot.data.domain.InvoiceListingRecord;
import io.fabric8.process.spring.boot.data.domain.InvoiceQuery;
import io.fabric8.process.spring.boot.data.repository.InvoiceRepository;
import io.fabric8.process.spring.boot.data.service.InvoiceListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@RequestMapping("/invoice-ops")
public class InvoiceRest {

    private final InvoiceRepository invoiceRepository;

    private final InvoiceListingService invoiceListingService;

    @Autowired
    public InvoiceRest(InvoiceRepository invoiceRepository, InvoiceListingService invoiceListingService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceListingService = invoiceListingService;
    }

    @RequestMapping(method = POST, value = "searchByQuery")
    @ResponseBody
    public Iterable<Invoice> searchByQuery(@RequestBody InvoiceQuery query) {
        return invoiceRepository.findByQuery(query);
    }

    @RequestMapping(method = POST, value = "countByQuery")
    @ResponseBody
    public long countByQuery(@RequestBody InvoiceQuery query) {
        return invoiceRepository.countByQuery(query);
    }

    @RequestMapping(method = POST, value = "listByQuery")
    @ResponseBody
    public Iterable<InvoiceListingRecord> listByQuery(@RequestBody InvoiceQuery query) {
        return invoiceListingService.listByQuery(query);
    }

}
