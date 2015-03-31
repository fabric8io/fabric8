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


import io.fabric8.cdi.qualifiers.Qualifiers;

import javax.enterprise.inject.spi.Producer;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceBean<X> extends ProducerBean<X> {

    private static final String SUFFIX = "-service";
    private static final Map<Key, ServiceBean> BEANS = new ConcurrentHashMap<>();
    
    private final String serviceId;
    private final String serviceProtocol;
    private final String serviceAlias;
    
    public static <S> ServiceBean<S> getBean(String id, String protocol, String alias, Class<S> type) {
        String serviceAlias = alias != null ? alias : id + "-" + type.getName() +"-"+ protocol + SUFFIX;
        Key key = new Key(id, protocol, serviceAlias, type, null);
        if (BEANS.containsKey(key)) {
            return BEANS.get(key);
        }
        ServiceBean bean = new ServiceBean(id, protocol, serviceAlias, type, null);
        BEANS.put(key, bean);
        return bean;
    }

    public static <S> ServiceBean<S> anyBean(String id, String protocol, Class<S> type) {
        for (Map.Entry<Key, ServiceBean> entry : BEANS.entrySet()) {
            Key key = entry.getKey();
            if (key.serviceId.equals(id) 
                    && key.serviceProtocol.equals(protocol)
                    && key.type.equals(type)) {
                return entry.getValue();
            }
        }
        return getBean(id, protocol, null, type);
    }
    
    
    public static final Collection<ServiceBean> getBeans() {
        return BEANS.values();
    }

    public static void doWith(Type type, Callback callback) {
        for (Map.Entry<Key, ServiceBean> entry : BEANS.entrySet()) {
            Key key = entry.getKey();
            if (type.equals(key.type)) {
                ServiceBean newBean = callback.apply(BEANS.remove(key));
                Key newKey = new Key(newBean.getId(), newBean.getServiceProtocol(), newBean.getServiceAlias(), newBean.getBeanClass(), newBean.getProducer());
                BEANS.put(newKey, newBean);
            }
        }
    }
    
    private ServiceBean(String serviceId, String serviceProtocol, String serviceAlias, Class type, Producer<X> producer) {
        super(serviceAlias, type, producer, Qualifiers.create(serviceId, serviceProtocol));
        this.serviceId = serviceId;
        this.serviceProtocol = serviceProtocol;
        this.serviceAlias = serviceAlias;
    }

    public ServiceBean withProducer(Producer producer) {
        return new ServiceBean(serviceId, serviceProtocol, serviceAlias, getBeanClass(), producer);
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

    private static final class Key {
        private final String serviceId;
        private final String serviceProtocol;
        private final String serviceAlias;
        private final Class type;
        private final Producer producer;


        private Key(String serviceId, String serviceProtocol, String serviceAlias, Class type, Producer producer) {
            this.serviceId = serviceId;
            this.serviceProtocol = serviceProtocol;
            this.serviceAlias = serviceAlias;
            this.type = type;
            this.producer = producer;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (producer != null ? !producer.equals(key.producer) : key.producer != null) return false;
            if (serviceId != null ? !serviceId.equals(key.serviceId) : key.serviceId != null) return false;
            if (serviceProtocol != null ? !serviceProtocol.equals(key.serviceProtocol) : key.serviceProtocol != null) return false;
            if (serviceAlias != null ? !serviceAlias.equals(key.serviceAlias) : key.serviceAlias != null)
                return false;
            if (type != null ? !type.equals(key.type) : key.type != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = serviceId != null ? serviceId.hashCode() : 0;
            result = 31 * result + (serviceProtocol != null ? serviceProtocol.hashCode() : 0);
            result = 31 * result + (serviceAlias != null ? serviceAlias.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (producer != null ? producer.hashCode() : 0);
            return result;
        }
    }
    
    public static interface Callback {
        public ServiceBean apply(ServiceBean bean);
        
    }
}
