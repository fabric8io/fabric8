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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import io.fabric8.agent.resolver.ResourceBuilder;
import io.fabric8.agent.resolver.ResourceImpl;
import io.fabric8.agent.resolver.ResourceUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

import static io.fabric8.agent.resolver.ResourceUtils.getUri;

public class TestBundle extends ResourceImpl implements BundleRevision, Bundle, BundleStartLevel {

    private final long bundleId;
    private final String location;
    private final Hashtable<String, String> headers = new Hashtable<>();
    public int state;

    public TestBundle(long bundleId, String location, int state, Hashtable<String, String> headers) throws BundleException {
        this.bundleId = bundleId;
        this.location = location;
        this.state = state;
        update(headers);
    }

    public void update(Hashtable<String, String> headers) throws BundleException {
        this.headers.clear();
        this.headers.putAll(headers);
        this.caps.clear();
        this.reqs.clear();
        ResourceBuilder.build(this, location, headers);
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public void start(int options) throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop(int options) throws BundleException {
        throw new UnsupportedOperationException();
   }

    @Override
    public void stop() throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(InputStream input) throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update() throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void uninstall() throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dictionary<String, String> getHeaders() {
        return headers;
    }

    @Override
    public long getBundleId() {
        return bundleId;
    }

    @Override
    public String getLocation() {
        return getUri(this);
    }

    @Override
    public ServiceReference<?>[] getRegisteredServices() {
        return new ServiceReference<?>[0];
    }

    @Override
    public ServiceReference<?>[] getServicesInUse() {
        return new ServiceReference<?>[0];
    }

    @Override
    public boolean hasPermission(Object permission) {
        return true;
    }

    @Override
    public URL getResource(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dictionary<String, String> getHeaders(String locale) {
        return getHeaders();
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<String> getEntryPaths(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL getEntry(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BundleContext getBundleContext() {
        return null;
    }

    @Override
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> A adapt(Class<A> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        return null;
    }

    @Override
    public File getDataFile(String filename) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSymbolicName() {
        return ResourceUtils.getSymbolicName(this);
    }

    @Override
    public Version getVersion() {
        return ResourceUtils.getVersion(this);
    }

    @Override
    public List<BundleCapability> getDeclaredCapabilities(String namespace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<BundleRequirement> getDeclaredRequirements(String namespace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTypes() {
        return 0;
    }

    @Override
    public BundleWiring getWiring() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getBundle() {
        return this;
    }

    @Override
    public int compareTo(Bundle o) {
        long thisBundleId = this.getBundleId();
        long thatBundleId = o.getBundleId();
        return (thisBundleId < thatBundleId ? -1 : (thisBundleId == thatBundleId ? 0 : 1));
    }

    @Override
    public int getStartLevel() {
        return 0;
    }

    @Override
    public void setStartLevel(int startlevel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPersistentlyStarted() {
        return false;
    }

    @Override
    public boolean isActivationPolicyUsed() {
        return false;
    }

}
