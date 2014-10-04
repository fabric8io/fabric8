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
package io.fabric8.process.spring.boot.data.service;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.fabric8.process.spring.boot.data.domain.Invoice;
import io.fabric8.process.spring.boot.data.domain.InvoiceListingRecord;
import io.fabric8.process.spring.boot.data.domain.InvoiceQuery;
import io.fabric8.process.spring.boot.data.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.springframework.beans.BeanUtils.copyProperties;

@Service
public class JpaInvoiceListingService implements InvoiceListingService {

    private final InvoiceRepository invoiceRepository;

    @Autowired
    public JpaInvoiceListingService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public Iterable<InvoiceListingRecord> listByQuery(InvoiceQuery query) {
        return Lists.newArrayList(Iterables.transform(invoiceRepository.findByQuery(query), new Function<Invoice, InvoiceListingRecord>() {
            @Override
            public InvoiceListingRecord apply(Invoice entity) {
                InvoiceListingRecord record = new InvoiceListingRecord();
                record.setListingLabel("Extra label for listing record.");
                copyProperties(entity, record);
                return record;
            }
        }));
    }

}
