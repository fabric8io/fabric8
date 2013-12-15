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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public final class Bundles {

    private Bundles() {
        //Utility Class
    }

    private static final transient Logger LOGGER = LoggerFactory.getLogger(Bundles.class);
    
    public static void startBundle(BundleContext context, String containsName) {
        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            String name = bundle.getSymbolicName();
            if (name.contains(containsName)) {
                LOGGER.debug("About to start bundle: " + name);
                try {
                    bundle.start();
                } catch (Exception e) {
                    LOGGER.warn("Failed to start bundle: " + name + " due " + e.getMessage() + ". This exception will be ignored.", e);
                }
            }
        }
    }

    public static void stopBundle(BundleContext context, String containsName) {
        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            String name = bundle.getSymbolicName();
            if (name.contains(containsName)) {
                LOGGER.debug("About to stop bundle: " + name);
                try {
                    bundle.stop();
                } catch (Exception e) {
                    LOGGER.warn("Failed to stop bundle: " + name + " due " + e.getMessage() + ". This exception will be ignored.", e);
                }
            }
        }
    }
}
