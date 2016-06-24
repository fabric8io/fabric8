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
package io.fabric8.cdi.qualifiers;

import io.fabric8.utils.Strings;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public final class Qualifiers {

    private Qualifiers() {
        //Utility
    }

    public static Annotation[] create(String serviceId, String protocol, String port, String path, Boolean endpoint, Boolean external) {
        if (serviceId == null) {
            throw new IllegalArgumentException("Service Id cannot be null.");
        }
        List<Annotation> qualifiers = new ArrayList<>();
        
        qualifiers.add(new ServiceNameQualifier(serviceId));
        if (!Strings.isNullOrBlank(protocol)) {
            qualifiers.add(new ProtocolQualifier(protocol));
        } else {
            qualifiers.add(new ProtocolQualifier(""));
        }

        if (!Strings.isNullOrBlank(port)) {
            qualifiers.add(new PortQualifier(port));
        } else {
            qualifiers.add(new PortQualifier(""));
        }

        if (!Strings.isNullOrBlank(path)) {
            qualifiers.add(new PathQualifier(path));
        } else {
            qualifiers.add(new PathQualifier(""));
        }

        qualifiers.add(new EndpointQualifier(endpoint));
        qualifiers.add(new ExternalQualifier(external));
        return qualifiers.toArray(new Annotation[qualifiers.size()]);
    }
}
