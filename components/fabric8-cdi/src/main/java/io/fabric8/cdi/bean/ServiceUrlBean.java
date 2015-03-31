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

    public static ServiceUrlBean getBean(String id, String protocol) {
        Key key = new Key(id, protocol);
        if (BEANS.containsKey(key)) {
            return BEANS.get(key);
        }
        ServiceUrlBean bean = new ServiceUrlBean(id, protocol);
        BEANS.put(key, bean);
        return bean;
    }

    public static Collection<ServiceUrlBean> getBeans() {
        return BEANS.values();
    }
    private final String serviceId;
    private final String serviceProtocol;

    private ServiceUrlBean(String serviceId, String serviceProtocol) {
        super(serviceProtocol + serviceId + SUFFIX, String.class, new ServiceUrlProducer(serviceId, serviceProtocol), Qualifiers.create(serviceId, serviceProtocol));
        this.serviceId = serviceId;
        this.serviceProtocol = serviceProtocol;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getServiceProtocol() {
        return serviceProtocol;
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

        private Key(String serviceId, String serviceProtocol) {
            this.serviceId = serviceId;
            this.serviceProtocol = serviceProtocol;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (serviceId != null ? !serviceId.equals(key.serviceId) : key.serviceId != null) return false;
            if (serviceProtocol != null ? !serviceProtocol.equals(key.serviceProtocol) : key.serviceProtocol != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = serviceId != null ? serviceId.hashCode() : 0;
            result = 31 * result + (serviceProtocol != null ? serviceProtocol.hashCode() : 0);
            return result;
        }
    }
}

