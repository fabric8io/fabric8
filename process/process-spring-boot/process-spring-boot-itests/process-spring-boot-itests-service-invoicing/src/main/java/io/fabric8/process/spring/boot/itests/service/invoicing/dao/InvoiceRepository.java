package io.fabric8.process.spring.boot.itests.service.invoicing.dao;

import io.fabric8.process.spring.boot.itests.service.invoicing.domain.Invoice;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "invoice", path = "invoice")
public interface InvoiceRepository extends PagingAndSortingRepository<Invoice, Long> {

    Invoice findByInvoiceId(String invoiceId);

}