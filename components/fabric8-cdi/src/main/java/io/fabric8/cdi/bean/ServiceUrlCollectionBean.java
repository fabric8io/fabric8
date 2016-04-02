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
import io.fabric8.cdi.producers.ServiceEndpointsProducer;
import io.fabric8.cdi.qualifiers.Qualifiers;
import io.fabric8.utils.Objects;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceUrlCollectionBean extends ProducerBean<List<String>> {

    private static final String SUFFIX = "urls";
    private static final Map<Key, ServiceUrlCollectionBean> BEANS = new HashMap<>();

    public static ServiceUrlCollectionBean getBean(String name, String protocol, String port, String path, String alias, Boolean endpoint, Boolean external, Type collectionType) {
        String serviceAlias = alias != null ? alias :
                Utils.toAlias(name, protocol, port, path, endpoint, external, SUFFIX);

        Key key = new Key(name, protocol, port, path, serviceAlias, endpoint, external, collectionType);
        if (BEANS.containsKey(key)) {
            return BEANS.get(key);
        }
        ServiceUrlCollectionBean bean = new ServiceUrlCollectionBean(name, protocol, port, path, serviceAlias, endpoint, external, collectionType);
        BEANS.put(key, bean);
        return bean;
    }

    public static ServiceUrlCollectionBean anyBean(String id, String protocol, String port, String path, Boolean endpoint, Boolean external, Type collectionType) {
        for (Map.Entry<Key, ServiceUrlCollectionBean> entry : BEANS.entrySet()) {
           Key key = entry.getKey();
            if (Objects.equal(key.serviceId, id)
                    && Objects.equal(key.serviceProtocol, protocol)
                    && Objects.equal(key.servicePort, port)
                    && Objects.equal(key.servicePath, path)
                    && Objects.equal(key.serviceEndpoint, endpoint)
                    && Objects.equal(key.serviceExternal, external)) {
               return entry.getValue();
           }
        }
        return getBean(id, protocol, port, path, null, endpoint, external, collectionType);
    }

    public static Collection<ServiceUrlCollectionBean> getBeans() {
        return BEANS.values();
    }
    
    private final String serviceName;
    private final String serviceProtocol;
    private final String servicePort;
    private final String servicePath;
    private final String serviceAlias;
    private final Boolean serviceEndpoint;
    private final Boolean serviceExternal;
    private final Type serviceCollectionType;

    private ServiceUrlCollectionBean(String serviceName, String serviceProtocol, String servicePort, String servicePath, String serviceAlias, Boolean serviceEndpoint, Boolean serviceExternal, Type serviceCollectionType) {
        super(serviceAlias, serviceCollectionType, new ServiceEndpointsProducer(serviceName, serviceProtocol, servicePort), Qualifiers.create(serviceName, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal));
        this.serviceName = serviceName;
        this.serviceProtocol = serviceProtocol;
        this.servicePort = servicePort;
        this.servicePath = servicePath;
        this.serviceAlias = serviceAlias;
        this.serviceEndpoint = serviceEndpoint;
        this.serviceExternal = serviceExternal;
        this.serviceCollectionType = serviceCollectionType;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceProtocol() {
        return serviceProtocol;
    }

    public String getServicePort() {
        return servicePort;
    }

    public String getServicePath() {
        return servicePath;
    }

    public String getServiceAlias() {
        return serviceAlias;
    }

    public Boolean isServiceEndpoint() {
        return serviceEndpoint;
    }

    public Boolean getServiceEndpoint() {
        return serviceEndpoint;
    }

    public Boolean getServiceExternal() {
        return serviceExternal;
    }

    public Type getServiceCollectionType() {
        return serviceCollectionType;
    }

    @Override
    public String toString() {
        return "ServiceUrlBean[" +
                "serviceName='" + serviceName + '\'' +
                ", serviceProtocol='" + serviceProtocol + '\'' +
                ", servicePort='" + servicePort + '\'' +
                ']';
    }

    private static final class Key {
        private final String serviceId;
        private final String serviceProtocol;
        private final String servicePort;
        private final String servicePath;
        private final String serviceAlias;
        private final Boolean serviceEndpoint;
        private final Boolean serviceExternal;
        private final Type serviceCollectionType;

        private Key(String serviceId, String serviceProtocol, String servicePort, String servicePath, String serviceAlias, Boolean serviceEndpoint, Boolean serviceExternal, Type serviceCollectionType) {
            this.serviceId = serviceId;
            this.serviceProtocol = serviceProtocol;
            this.servicePort = servicePort;
            this.servicePath = servicePath;
            this.serviceAlias = serviceAlias;
            this.serviceEndpoint = serviceEndpoint;
            this.serviceExternal = serviceExternal;
            this.serviceCollectionType = serviceCollectionType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (serviceId != null ? !serviceId.equals(key.serviceId) : key.serviceId != null) return false;
            if (serviceProtocol != null ? !serviceProtocol.equals(key.serviceProtocol) : key.serviceProtocol != null) return false;
            if (servicePort != null ? !servicePort.equals(key.servicePort) : key.servicePort != null) return false;
            if (servicePath != null ? !servicePath.equals(key.servicePath) : key.servicePath != null) return false;
            if (serviceAlias != null ? !serviceAlias.equals(key.serviceProtocol) : key.serviceAlias != null) return false;
            if (serviceEndpoint != null ? !serviceEndpoint.equals(key.serviceEndpoint) : key.serviceEndpoint != null) return false;
            if (serviceExternal != null ? !serviceExternal.equals(key.serviceExternal) : key.serviceExternal != null) return false;
            if (serviceCollectionType != null ? !serviceCollectionType.equals(key.serviceCollectionType) : key.serviceCollectionType != null) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = serviceId != null ? serviceId.hashCode() : 0;
            result = 31 * result + (serviceProtocol != null ? serviceProtocol.hashCode() : 0);
            result = 31 * result + (servicePort != null ? servicePort.hashCode() : 0);
            result = 31 * result + (servicePath != null ? servicePath.hashCode() : 0);
            result = 31 * result + (serviceAlias != null ? serviceAlias.hashCode() : 0);
            result = 31 * result + (serviceEndpoint != null ? serviceEndpoint.hashCode() : 0);
            result = 31 * result + (serviceExternal != null ? serviceExternal.hashCode() : 0);
            result = 31 * result + (serviceCollectionType != null ? serviceCollectionType.hashCode() : 0);
            return result;
        }
    }
}

