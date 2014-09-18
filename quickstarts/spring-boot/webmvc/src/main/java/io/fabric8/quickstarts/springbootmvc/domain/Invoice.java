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
package io.fabric8.quickstarts.springbootmvc.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.LinkedList;
import java.util.List;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.GenerationType.AUTO;

@Entity
@JsonIgnoreProperties({"_links"})
public class Invoice {

    // Members

    @Id
    @GeneratedValue(strategy = AUTO)
    private long id;

    private String invoiceId;

    @OneToMany(cascade = ALL)
    private List<InvoiceCorrection> corrections = new LinkedList<InvoiceCorrection>();

    // Getters & setters

    public long id() {
        return id;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public String invoiceId() {
        return this.invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Invoice invoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
        return this;
    }

    public List<InvoiceCorrection> getCorrections() {
        return corrections;
    }

    public List<InvoiceCorrection> corrections() {
        return corrections;
    }

    public void setCorrections(List<InvoiceCorrection> corrections) {
        this.corrections = corrections;
    }

    public Invoice addCorrection(InvoiceCorrection invoiceCorrection) {
        corrections.add(invoiceCorrection);
        return this;
    }

}
