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
package io.fabric8.gateway.model;

import io.fabric8.gateway.support.UriTemplate;

import java.util.concurrent.atomic.AtomicReference;

/**
 */
public class UriTemplateDefinition {
    private String uriTemplate;
    private AtomicReference<UriTemplate> uriTemplateReference = new AtomicReference<UriTemplate>();

    public UriTemplateDefinition() {
    }

    public UriTemplateDefinition(String uriTemplate) {
        this.uriTemplate = uriTemplate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UriTemplateDefinition that = (UriTemplateDefinition) o;

        if (uriTemplate != null ? !uriTemplate.equals(that.uriTemplate) : that.uriTemplate != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return uriTemplate != null ? uriTemplate.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "UriTemplateDefinition{" +
                "uriTemplate='" + uriTemplate + '\'' +
                '}';
    }

    // Properties
    //-------------------------------------------------------------------------

    /**
     * Returns the URI template mapping the URI to the underlying back end service.
     */
    public String getUriTemplate() {
        return uriTemplate;
    }

    public void setUriTemplate(String uriTemplate) {
        this.uriTemplate = uriTemplate;
        uriTemplateReference.set(null);
    }

    /**
     * Returns the {@link io.fabric8.gateway.support.UriTemplate} instance which is lazily constructed
     * from the {@link #getUriTemplate()} value.
     */
    public UriTemplate getUriTemplateObject() {
        uriTemplateReference.compareAndSet(null, new UriTemplate(getUriTemplate()));
        return uriTemplateReference.get();
    }
}
