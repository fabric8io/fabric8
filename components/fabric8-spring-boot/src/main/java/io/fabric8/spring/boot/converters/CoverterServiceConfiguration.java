/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.spring.boot.converters;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.GenericConverter;

import java.util.Set;

@Configuration
public class CoverterServiceConfiguration {

    @Bean
    ServiceConverter serviceConverter(KubernetesClient client) {
        ServiceConverter converter =  new ServiceConverter();
        converter.setKubernetesClient(client);
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean(ConversionService.class)
    public ConversionService conversionService(Set<GenericConverter> genericConverters) {
        ConversionServiceFactoryBean bean = new ConversionServiceFactoryBean();
        bean.setConverters(genericConverters);
        bean.afterPropertiesSet();
        return bean.getObject();
    }
}
