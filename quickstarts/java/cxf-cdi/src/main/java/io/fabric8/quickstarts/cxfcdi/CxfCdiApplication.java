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
package io.fabric8.quickstarts.cxfcdi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import io.fabric8.cxf.endpoint.EnableJMXFeature;
import io.fabric8.cxf.endpoint.SwaggerFeature;
import org.apache.cxf.feature.LoggingFeature;


@ApplicationPath( "cxfcdi" )
public class CxfCdiApplication extends Application {
    @Inject private CustomerService customerService;
    @Produces private JacksonJsonProvider jacksonJsonProvider = new JacksonJsonProvider();

    @Override
    public Set<Object> getSingletons() {
            return new HashSet<Object>(
                    Arrays.asList(
                    customerService,
                    jacksonJsonProvider,
                    new SwaggerFeature(),
                    new EnableJMXFeature(),
                    new LoggingFeature()
                )
        );
    }
}

