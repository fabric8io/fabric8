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
package io.fabric8.cxf.endpoint;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.InterceptorProvider;

public class SwaggerFeature extends org.apache.cxf.jaxrs.swagger.SwaggerFeature {
    
    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        if (provider instanceof Endpoint) {
            EndpointImpl endpointImpl = (EndpointImpl)provider;
            List<Feature> features = endpointImpl.getActiveFeatures();
            if (features == null) {
                features = new ArrayList<Feature>();
                features.add(this);
                endpointImpl.initializeActiveFeatures(features);
            } else {
                features.add(this);
            }
        }
    }

}
