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
package io.fabric8.fab.osgi.itests;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;

/**
 * A stub BundleContext for running code outside of OSGi
 */
class StubBundleContext implements BundleContext {

    public void addBundleListener(BundleListener listener) {
    }

    public String getProperty(String key) {
        return System.getProperty(key);
    }

    public Bundle getBundle() {
        // TODO
        return null;
    }

    public Bundle installBundle(String location, InputStream input) throws BundleException {
        return null;
    }

    public Bundle installBundle(String location) throws BundleException {
        return null;
    }

    public Bundle getBundle(long id) {
        return null;
    }

    public Bundle[] getBundles() {
        return new Bundle[0];
    }

    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
    }

    public void addServiceListener(ServiceListener listener) {
    }

    public void removeServiceListener(ServiceListener listener) {
    }

    public void removeBundleListener(BundleListener listener) {
    }

    public void addFrameworkListener(FrameworkListener listener) {
    }

    public void removeFrameworkListener(FrameworkListener listener) {
    }

    public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
        return null;
    }

    public ServiceRegistration registerService(String clazz, Object service, Dictionary properties) {
        return null;
    }

    public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        return new ServiceReference[0];
    }

    public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        return new ServiceReference[0];
    }

    public ServiceReference getServiceReference(String clazz) {
        return null;
    }

    public Object getService(ServiceReference reference) {
        return null;
    }

    public boolean ungetService(ServiceReference reference) {
        return false;
    }

    public File getDataFile(String filename) {
        return null;
    }

    public Filter createFilter(String filter) throws InvalidSyntaxException {
        return null;
    }
}
