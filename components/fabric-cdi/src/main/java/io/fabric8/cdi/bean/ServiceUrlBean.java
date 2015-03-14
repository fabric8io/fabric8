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
import io.fabric8.cdi.qualifiers.ServiceQualifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ServiceUrlBean extends ProducerBean<String> {

    private static final String SUFFIX = "-service-url-bean";
    private static final Map<String, ServiceUrlBean> BEANS = new HashMap<>();

    public static ServiceUrlBean getBean(String key) {
        if (BEANS.containsKey(key)) {
            return BEANS.get(key);
        }
        ServiceUrlBean bean = new ServiceUrlBean(key);
        BEANS.put(key, bean);
        return bean;
    }

    public static Collection<ServiceUrlBean> getBeans() {
        return BEANS.values();
    }
    private final String serviceId;

    private ServiceUrlBean(String serviceId) {
        super(serviceId + SUFFIX, String.class, new ServiceUrlProducer(serviceId), new ServiceQualifier(serviceId));
        this.serviceId = serviceId;
    }

    public String getServiceId() {
        return serviceId;
    }
}

