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
package io.fabric8.insight.camel.base;

import org.apache.camel.CamelContext;
import org.apache.camel.Service;
import org.apache.camel.management.DefaultManagementNamingStrategy;
import org.apache.camel.util.ObjectHelper;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 *
 */
public class InsightManagementNamingStrategy extends DefaultManagementNamingStrategy {

    public static final String TYPE_FABRIC = "fabric";
    public static final String TYPE_INSIGHT = "insight";

    public InsightManagementNamingStrategy(String domainName) {
        super(domainName);
    }

    @Override
    public ObjectName getObjectNameForService(CamelContext context, Service service) throws MalformedObjectNameException {
        StringBuilder buffer = new StringBuilder();
        buffer.append(domainName).append(":");
        buffer.append(KEY_CONTEXT + "=").append(getContextId(context)).append(",");
        // special for fabric
        if (service.getClass().getCanonicalName().startsWith("org.apache.camel.fabric")) {
            buffer.append(KEY_TYPE + "=" + TYPE_FABRIC + ",");
        } else if (service.getClass().getCanonicalName().startsWith("io.fabric8.insight.camel")) {
            buffer.append(KEY_TYPE + "=" + TYPE_INSIGHT + ",");
        } else {
            buffer.append(KEY_TYPE + "=" + TYPE_SERVICE + ",");
        }
        buffer.append(KEY_NAME + "=")
                .append(service.getClass().getSimpleName())
                .append("(").append(ObjectHelper.getIdentityHashCode(service)).append(")");
        return createObjectName(buffer);
    }

}
