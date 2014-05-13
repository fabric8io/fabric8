package io.fabric8.process.spring.boot.itests.service.invoicing.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@JsonIgnoreProperties({"_links"})
public class InvoiceCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private long netValue;


    public long getNetValue() {
        return netValue;
    }

    public void setNetValue(long netValue) {
        this.netValue = netValue;
    }

    public InvoiceCorrection netValue(long netValue) {
        this.netValue = netValue;
        return this;
    }

}