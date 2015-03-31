/*
 * Copyright 2005-2014 Red Hat, Inc.                                    
 *                                                                      
 * Red Hat licenses this file to you under the Apache License, version  
 * 2.0 (the "License"); you may not use this file except in compliance  
 * with the License.  You may obtain a copy of the License at           
 *                                                                      
 *    http://www.apache.org/licenses/LICENSE-2.0                        
 *                                                                      
 * Unless required by applicable law or agreed to in writing, software  
 * distributed under the License is distributed on an "AS IS" BASIS,    
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or      
 * implied.  See the License for the specific language governing        
 * permissions and limitations under the License.
 */

package io.fabric8.cdi.bean;


import io.fabric8.cdi.producers.ServiceUrlProducer;
import io.fabric8.cdi.qualifiers.Qualifiers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ServiceUrlBean extends ProducerBean<String> {

    private static final String SUFFIX = "-url";
    private static final Map<Key, ServiceUrlBean> BEANS = new HashMap<>();

    public static ServiceUrlBean getBean(String id, String protocol, String alias) {
        String serviceAlias = alias != null ? alias : id + "-" + protocol + SUFFIX;
        Key key = new Key(id, protocol, serviceAlias);
        if (BEANS.containsKey(key)) {
            return BEANS.get(key);
        }
        ServiceUrlBean bean = new ServiceUrlBean(id, protocol, serviceAlias);
        BEANS.put(key, bean);
        return bean;
    }

    public static ServiceUrlBean anyBean(String id, String protocol) {
        for (Map.Entry<Key, ServiceUrlBean> entry : BEANS.entrySet()) {
           Key key = entry.getKey();
           if (key.serviceId.equals(id) && key.serviceProtocol.equals(protocol)) {
               return entry.getValue();
           }
        }
        return getBean(id, protocol, null);
    }

    public static Collection<ServiceUrlBean> getBeans() {
        return BEANS.values();
    }
    private final String serviceId;
    private final String serviceProtocol;
    private final String serviceAlias;

    private ServiceUrlBean(String serviceId, String serviceProtocol, String serviceAlias) {
        super(serviceAlias, String.class, new ServiceUrlProducer(serviceId, serviceProtocol), Qualifiers.create(serviceId, serviceProtocol));
        this.serviceId = serviceId;
        this.serviceProtocol = serviceProtocol;
        this.serviceAlias = serviceAlias;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getServiceProtocol() {
        return serviceProtocol;
    }

    public String getServiceAlias() {
        return serviceAlias;
    }

    @Override
    public String toString() {
        return "ServiceUrlBean[" +
                "serviceId='" + serviceId + '\'' +
                ", serviceProtocol='" + serviceProtocol + '\'' +
                ']';
    }

    private static final class Key {
        private final String serviceId;
        private final String serviceProtocol;
        private final String serviceAlias;

        private Key(String serviceId, String serviceProtocol, String serviceAlias) {
            this.serviceId = serviceId;
            this.serviceProtocol = serviceProtocol;
            this.serviceAlias = serviceAlias;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (serviceId != null ? !serviceId.equals(key.serviceId) : key.serviceId != null) return false;
            if (serviceProtocol != null ? !serviceProtocol.equals(key.serviceProtocol) : key.serviceProtocol != null) return false;
            if (serviceAlias != null ? !serviceAlias.equals(key.serviceProtocol) : key.serviceAlias != null) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = serviceId != null ? serviceId.hashCode() : 0;
            result = 31 * result + (serviceProtocol != null ? serviceProtocol.hashCode() : 0);
            result = 31 * result + (serviceAlias != null ? serviceAlias.hashCode() : 0);
            return result;
        }
    }
}

