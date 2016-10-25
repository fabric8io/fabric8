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

import java.util.List;

import io.fabric8.karaf.checks.Check;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public abstract class AbstractChecker implements io.fabric8.karaf.checks.HealthChecker,
                                                       io.fabric8.karaf.checks.ReadinessChecker {

    protected final Bundle bundle;
    protected final BundleContext bundleContext;
    protected final Bundle systemBundle;
    protected final BundleContext systemBundleContext;

    public AbstractChecker() {
        bundle = FrameworkUtil.getBundle(getClass());
        bundleContext = bundle.getBundleContext();
        systemBundle= bundleContext.getBundle(0);
        systemBundleContext = systemBundle.getBundleContext();
    }

    @Override
    public List<Check> getFailingHealthChecks() {
        return doCheck();
    }

    @Override
    public List<Check> getFailingReadinessChecks() {
        return doCheck();
    }

    protected abstract List<Check> doCheck();


}
