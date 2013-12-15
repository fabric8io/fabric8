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
package io.fabric8.fab.osgi.util;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;

/**
 * Helper class for working with the OSGi ConfigurationAdmin service.
 */
public class ConfigurationAdminHelper {

    /**
     * Extract the properties defined in a ConfigurationAdmin PID.  If the service is unavailable at the time,
     * this method will not return null but it will provide an empty Dictionary instead.
     *
     * @param context the bundle context
     * @param pid the ConfigurationAdmin PID
     * @return the properties defined in the PID or an empty dictionary
     */
    public static Dictionary getProperties(BundleContext context, String pid) {
        ServiceReference reference = context.getServiceReference(ConfigurationAdmin.class.getName());
        if (reference == null) {
            return emptyDictionary();
        }
        try {
            return getProperties((ConfigurationAdmin) context.getService(reference), pid);
        } finally {
            context.ungetService(reference);
        }
    }

    /**
     * Extract the properties defined in a ConfigurationAdmin PID.  If the service is unavailable at the time,
     * this method will not return null but it will provide an empty Dictionary instead.
     *
     * @param admin the ConfigurationAdmin service
     * @param pid the ConfigurationAdmin PID
     * @return the properties defined in the PID or an empty dictionary
     */
    public static Dictionary getProperties(ConfigurationAdmin admin, String pid) {
        if (admin == null) {
            return emptyDictionary();
        }
        try {
            return getProperties(admin.getConfiguration(pid));
        } catch (IOException e) {
            return emptyDictionary();
        }
    }

    /*
     * Extract the properties from a Configuration object
     */
    private static Dictionary getProperties(Configuration configuration) {
        if (configuration == null) {
            return emptyDictionary();
        } else {
            return configuration.getProperties();
        }
    }

    /*
     * Create an empty dictionary instance
     */
    private static Dictionary emptyDictionary() {
        return new Properties();
    }
}
