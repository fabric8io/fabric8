/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.fabric8.utils;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class BundleUtils {

    private final BundleContext syscontext;

    public BundleUtils(BundleContext context) {
        this.syscontext = context.getBundle(0).getBundleContext();
    }

    public Bundle installBundle(String location) throws BundleException {
            return syscontext.installBundle(location);
    }

    public Bundle findBundle(String bsn) throws BundleException {
        for (Bundle b : syscontext.getBundles()) {
            if (b.getSymbolicName() != null && b.getSymbolicName().equals(bsn)) {
                return b;
            }
        }
        return null;
    }

    public Bundle findAndStopBundle(String bsn) throws BundleException {
        Bundle bundle = findBundle(bsn);
        if (bundle != null) {
            bundle.stop();
        }
        return bundle;
    }
}
