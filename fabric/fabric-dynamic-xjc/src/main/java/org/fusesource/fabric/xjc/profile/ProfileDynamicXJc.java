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
package org.fusesource.fabric.xjc.profile;

import java.io.IOException;
import java.util.Dictionary;

import org.apache.curator.framework.CuratorFramework;
import org.fusesource.common.util.Dictionaries;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches a profiles configuration and dynamically creates the JAXB beans
 * for all the XSDs stored within the profiles configuration.
 */
public class ProfileDynamicXJc implements ManagedServiceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ProfileDynamicXJc.class);

    private static final String NAME = "Profile Dynamic XJC";

    private static final String SCHEMAS_PATH_PROPERTY_NAME = "schemas.path";

    private CuratorFramework curator;
    private BundleContext bundleContext;
    private SchemaDirectoryWatcher directoryWatcher;

    public synchronized void init() throws IOException {
        // TODO try force an update event?
        LOG.info("ProfileDynamicXJc init()");
        System.out.println("ProfileDynamicXJc init()");
    }

    public synchronized void destroy() throws IOException {
        if (directoryWatcher != null) {
            directoryWatcher.stop();
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties != null) {
            try {
                String schemasPath = Dictionaries
                        .readString(properties, SCHEMAS_PATH_PROPERTY_NAME, "schemas");


                LOG.info("ProfileDynamicXJc detected change in the schemas, so reloading from path "
                        + schemasPath);
                System.out.println("ProfileDynamicXJc detected change in the schemas, so reloading from path "
                        + schemasPath);

                if (directoryWatcher != null) {
                    directoryWatcher.stop();
                }
                directoryWatcher = new SchemaDirectoryWatcher(this, schemasPath);
                directoryWatcher.start();

            } catch (Exception e) {
                LOG.error("Failed to process properties " + properties + ". " + e, e);
            }
        }
    }

    @Override
    public void deleted(String pid) {
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
