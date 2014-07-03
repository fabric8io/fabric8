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
package io.fabric8.process.spring.boot.data.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.LinkedList;
import java.util.List;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.GenerationType.AUTO;

@Entity
@JsonIgnoreProperties({"_links"})
@Data
public class Invoice {

    // Members

    @Id
    @GeneratedValue(strategy = AUTO)
    private long id;

    private String invoiceId;

    @OneToMany(cascade = ALL)
    private List<InvoiceCorrection> corrections = new LinkedList<InvoiceCorrection>();

    // Fluent getters & setters

    public long id() {
        return id;
    }

    public String invoiceId() {
        return this.invoiceId;
    }

    public Invoice invoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
        return this;
    }

    public List<InvoiceCorrection> corrections() {
        return corrections;
    }

    public Invoice addCorrection(InvoiceCorrection invoiceCorrection) {
        corrections.add(invoiceCorrection);
        return this;
    }

}
