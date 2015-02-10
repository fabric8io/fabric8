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


import io.fabric8.cdi.annotations.Service;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Annotation;

public class ServiceBean extends BaseBean<io.fabric8.kubernetes.api.model.Service> {

    private final io.fabric8.kubernetes.api.model.Service service;

    public ServiceBean(io.fabric8.kubernetes.api.model.Service service) {
        super(service.getId(), new ServiceQualifier(service.getId()));
        this.service = service;
    }


    @Override
    public String getName() {
        return service.getId();
    }

    @Override
    public io.fabric8.kubernetes.api.model.Service create(CreationalContext<io.fabric8.kubernetes.api.model.Service> creationalContext) {
        return service;
    }

    @Override
    public void destroy(io.fabric8.kubernetes.api.model.Service instance, CreationalContext<io.fabric8.kubernetes.api.model.Service> creationalContext) {

    }
    
    private static class ServiceQualifier extends AnnotationLiteral<Service> implements Service {
        private final String value;

        private ServiceQualifier(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}
