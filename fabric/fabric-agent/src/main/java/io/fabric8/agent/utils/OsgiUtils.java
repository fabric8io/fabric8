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
package io.fabric8.agent.utils;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

public class OsgiUtils {

    public static void ensureAllClassesLoaded(Bundle bundle) throws ClassNotFoundException {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        if (wiring != null) {
            for (String path : wiring.listResources("/", "*.class", BundleWiring.LISTRESOURCES_RECURSE)) {
                String className = path.substring(0, path.length() - ".class".length());
                className = className.replace('/', '.');
                bundle.loadClass(className);
            }
        }
    }

}
