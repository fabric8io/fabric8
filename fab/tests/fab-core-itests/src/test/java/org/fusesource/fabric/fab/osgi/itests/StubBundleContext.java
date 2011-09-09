/**
 * Copyright (C) 2010, FuseSource Corp. All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.itests;

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
