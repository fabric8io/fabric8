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

package io.fabric8.spring.boot.converters;

import io.fabric8.kubernetes.api.model.Service;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

@Component
public class ServiceToUrl implements Converter<Service, URL> {

    private final ServiceToString serviceToString;

    public ServiceToUrl(ServiceToString serviceToString) {
        this.serviceToString = serviceToString;
    }


    @Override
    public URL convert(Service source) {
        try {
            return new URL(serviceToString.convert(source));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
