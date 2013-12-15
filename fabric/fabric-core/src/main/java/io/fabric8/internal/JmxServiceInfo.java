/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.internal;

import java.util.Collection;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import io.fabric8.api.data.ServiceInfo;
import org.osgi.jmx.JmxConstants;

import static org.osgi.jmx.framework.ServiceStateMBean.BUNDLE_IDENTIFIER;
import static org.osgi.jmx.framework.ServiceStateMBean.IDENTIFIER;
import static org.osgi.jmx.framework.ServiceStateMBean.OBJECT_CLASS;
import static org.osgi.jmx.framework.ServiceStateMBean.USING_BUNDLES;

/**
 * Implementation of ServiceInfo interface based on CompositeData.
 */
public class JmxServiceInfo extends JmxInfo implements ServiceInfo {

    protected final TabularData properties;

    public JmxServiceInfo(CompositeData data, TabularData properties) {
        super(data, IDENTIFIER);
        this.properties = properties;
    }

    public Long getBundleId() {
        return (Long) data.get(BUNDLE_IDENTIFIER);
    }

    public Long[] getUsingBundlesId() {
        return (Long[]) data.get(USING_BUNDLES);
    }

    public String[] getObjectClasses() {
        return (String[]) data.get(OBJECT_CLASS);
    }

    @Override
    public Property[] getProperties() {
        Property[] props = new Property[this.properties.size()];
        int i = 0;
        for (CompositeData data : (Collection<CompositeData>) properties.values()) {
            String key = data.get(JmxConstants.KEY).toString();
            Object value = data.get(JmxConstants.VALUE);
            props[i++] = new PropertyImpl(key, value);
        }
        return props;
    }

    static class PropertyImpl implements Property {
        final String key;
        final Object value;

        PropertyImpl(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }
    }
}
