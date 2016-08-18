/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.karaf.checks.internal;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.karaf.checks.Check;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRevision;

public abstract class AbstractBundleChecker extends AbstractChecker {

    @Override
    protected List<Check> doCheck() {
        List<Check> checks = new ArrayList<>();
        for (Bundle b : systemBundleContext.getBundles()) {
            Check check = checkBundle(b);
            if (check != null) {
                checks.add(check);
            }
        }
        return checks;
    }

    protected abstract Check checkBundle(Bundle bundle);

    protected boolean isActive(Bundle bundle) {
        if (bundle.getState() == Bundle.ACTIVE) {
            return true;
        }
        if (bundle.getState() == Bundle.STARTING) {
            String activationPolicyHeader = bundle.getHeaders().get(Constants.BUNDLE_ACTIVATIONPOLICY);
            if (activationPolicyHeader != null && activationPolicyHeader.startsWith(Constants.ACTIVATION_LAZY)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isFragment(Bundle bundle) {
        return (bundle.adapt(BundleRevision.class).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
    }
}
