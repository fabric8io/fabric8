/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.service;

import java.util.Collection;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;

public class TestSystemBundle extends TestBundle implements FrameworkStartLevel, FrameworkWiring {

    private BundleContext bundleContext;

    public TestSystemBundle(Hashtable<String, String> headers) throws BundleException {
        super(0l, "system-bundle", Bundle.ACTIVE, headers);
        update(headers);
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public void setStartLevel(int startlevel, FrameworkListener... listeners) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInitialBundleStartLevel() {
        return 0;
    }

    @Override
    public void setInitialBundleStartLevel(int startlevel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refreshBundles(Collection<Bundle> bundles, FrameworkListener... listeners) {
    }

    @Override
    public boolean resolveBundles(Collection<Bundle> bundles) {
        return false;
    }

    @Override
    public Collection<Bundle> getRemovalPendingBundles() {
        return null;
    }

    @Override
    public Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles) {
        return null;
    }
}
