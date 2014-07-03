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
package io.fabric8.process.spring.boot.data.repository;

import io.fabric8.process.spring.boot.data.domain.Invoice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public class InvoiceRepositoryImpl implements InvoiceRepositoryCustom {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Override
    public Iterable<Invoice> findByQuery(InvoiceQuery query) {
        return invoiceRepository.findAll(queryInvoices(query), query.pageRequest()).getContent();
    }

    @Override
    public long countByQuery(InvoiceQuery query) {
        return invoiceRepository.count(queryInvoices(query));
    }

    private static Specification<Invoice> queryInvoices(final InvoiceQuery query) {
        return new Specification<Invoice>() {
            @Override
            public Predicate toPredicate(Root<Invoice> root, CriteriaQuery<?> qq, CriteriaBuilder cb) {
                if (query.getInvoiceId() != null) {
                    return cb.equal(root.get("invoiceId"), query.getInvoiceId());
                } else {
                    return cb.isNull(root.get("invoiceId"));
                }
            }
        };
    }

}
