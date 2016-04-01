/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.cdi.bean;


import io.fabric8.cdi.Utils;
import io.fabric8.cdi.qualifiers.Qualifiers;
import io.fabric8.utils.Objects;

import javax.enterprise.inject.spi.Producer;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceBean<X> extends ProducerBean<X> {

    private static final Map<Key, ServiceBean> BEANS = new ConcurrentHashMap<>();
    
    private final String serviceName;
    private final String serviceProtocol;
    private final String servicePort;
    private final String servicePath;
    private final String serviceAlias;
    private final Boolean serviceEndpoint;
    private final Boolean serviceExternal;
    
    public static <S> ServiceBean<S> getBean(String name, String protocol, String port, String path, String alias, Boolean endpoint, Boolean external, Type type) {
        String serviceAlias = alias != null ? alias :
                Utils.toAlias(name, protocol, port, path, endpoint, external, "bean-" + type.toString());

        Key key = new Key(name, protocol, port, path, serviceAlias, endpoint, external, type, null);
        if (BEANS.containsKey(key)) {
            return BEANS.get(key);
        }
        ServiceBean bean = new ServiceBean(name, protocol, port, path, serviceAlias, type, null, endpoint, external);
        BEANS.put(key, bean);
        return bean;
    }

    public static <S> ServiceBean<S> anyBean(String id, String protocol, String port, String path, Boolean endpoint, Boolean external, Type type) {
        for (Map.Entry<Key, ServiceBean> entry : BEANS.entrySet()) {
            Key key = entry.getKey();
            if (Objects.equal(key.serviceName, id)
                    && Objects.equal(key.serviceProtocol, protocol)
                    && Objects.equal(key.servicePort, port)
                    && Objects.equal(key.servicePath, path)
                    && Objects.equal(key.serviceEndpoint, endpoint)
                    && Objects.equal(key.serviceExternal, external)
                    && Objects.equal(key.type, type)) {
                return entry.getValue();
            }
        }
        return getBean(id, protocol, port, path, null, endpoint, external, type);
    }
    
    
    public static final Collection<ServiceBean> getBeans() {
        return BEANS.values();
    }

    public static void doWith(Type type, Callback callback) {
        for (Map.Entry<Key, ServiceBean> entry : BEANS.entrySet()) {
            Key key = entry.getKey();
            if (type.equals(key.type)) {
                ServiceBean newBean = callback.apply(BEANS.remove(key));
                Key newKey = new Key(newBean.getServiceName(), newBean.getServiceProtocol(), newBean.getServicePort(), newBean.getServicePath(), newBean.getServiceAlias(), newBean.getServiceEndpoint(), newBean.getServiceExternal(), newBean.getBeanClass(), newBean.getProducer());
                BEANS.put(newKey, newBean);
            }
        }
    }
    
    private ServiceBean(String serviceName, String serviceProtocol, String servicePort, String servicePath, String serviceAlias, Type type, Producer<X> producer, Boolean serviceEndpoint, Boolean serviceExternal) {
        super(serviceAlias, type, producer, Qualifiers.create(serviceName, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal));
        this.serviceName = serviceName;
        this.serviceProtocol = serviceProtocol;
        this.servicePort = servicePort;
        this.servicePath = servicePath;
        this.serviceAlias = serviceAlias;
        this.serviceEndpoint = serviceEndpoint;
        this.serviceExternal = serviceExternal;
    }

    public ServiceBean withProducer(Producer producer) {
        return new ServiceBean(serviceName, serviceProtocol, servicePort, servicePath, serviceAlias, getBeanClass(), producer, serviceEndpoint, serviceExternal);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceProtocol() {
        return serviceProtocol;
    }

    public String getServiceAlias() {
        return serviceAlias;
    }

    public String getServicePort() {
        return servicePort;
    }


    public String getServicePath() {
        return servicePath;
    }

    public Boolean getServiceEndpoint() {
        return serviceEndpoint;
    }

    public Boolean getServiceExternal() {
        return serviceExternal;
    }

    @Override
    public String toString() {
        return "ServiceBean[" +
                "serviceName='" + serviceName + '\'' +
                ", serviceProtocol='" + serviceProtocol + '\'' +
                ", servicePort='" + servicePort + '\'' +
                ", servicePath='" + servicePath + '\'' +
                ", serviceAlias='" + serviceAlias + '\'' +
                ", serviceEndpoint=" + serviceEndpoint +
                ", serviceExternal=" + serviceExternal +
                ']';
    }

    private static final class Key {
        private final String serviceName;
        private final String serviceProtocol;
        private final String servicePort;
        private final String servicePath;
        private final String serviceAlias;
        private final Boolean serviceEndpoint;
        private final Boolean serviceExternal;
        private final Type type;
        private final Producer producer;


        private Key(String serviceName, String serviceProtocol, String servicePort, String servicePath, String serviceAlias, Boolean serviceEndpoint, Boolean serviceExternal, Type type, Producer producer) {
            this.serviceName = serviceName;
            this.serviceProtocol = serviceProtocol;
            this.servicePath = servicePath;
            this.serviceAlias = serviceAlias;
            this.servicePort = servicePort;
            this.serviceEndpoint = serviceEndpoint;
            this.serviceExternal = serviceExternal;
            this.type = type;
            this.producer = producer;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (producer != null ? !producer.equals(key.producer) : key.producer != null) return false;
            if (serviceName != null ? !serviceName.equals(key.serviceName) : key.serviceName != null) return false;
            if (serviceProtocol != null ? !serviceProtocol.equals(key.serviceProtocol) : key.serviceProtocol != null) return false;
            if (servicePort != null ? !servicePort.equals(key.servicePort) : key.servicePort != null) return false;
            if (servicePath != null ? !servicePath.equals(key.servicePath) : key.servicePath != null) return false;
            if (serviceAlias != null ? !serviceAlias.equals(key.serviceAlias) : key.serviceAlias != null) return false;
            if (serviceEndpoint != null ? !serviceEndpoint.equals(key.serviceEndpoint) : key.serviceEndpoint != null) return false;
            if (serviceExternal != null ? !serviceExternal.equals(key.serviceExternal) : key.serviceExternal != null) return false;
            if (type != null ? !type.equals(key.type) : key.type != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = serviceName != null ? serviceName.hashCode() : 0;
            result = 31 * result + (serviceProtocol != null ? serviceProtocol.hashCode() : 0);
            result = 31 * result + (servicePort != null ? servicePort.hashCode() : 0);
            result = 31 * result + (servicePath != null ? servicePath.hashCode() : 0);
            result = 31 * result + (serviceAlias != null ? serviceAlias.hashCode() : 0);
            result = 31 * result + (serviceExternal != null ? serviceExternal.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (producer != null ? producer.hashCode() : 0);
            return result;
        }
    }
    
    public static interface Callback {
        public ServiceBean apply(ServiceBean bean);
        
    }
}
