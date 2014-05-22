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