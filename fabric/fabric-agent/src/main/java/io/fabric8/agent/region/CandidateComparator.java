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
package io.fabric8.agent.region;

import java.util.Comparator;
import java.util.Set;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class CandidateComparator implements Comparator<Capability> {

    private final Set<Resource> mandatory;

    public CandidateComparator(Set<Resource> mandatory) {
        this.mandatory = mandatory;
    }

    public int compare(Capability cap1, Capability cap2) {
        int c = 0;
        // Always prefer mandatory resources
        if (c == 0) {
            if (mandatory.contains(cap1.getResource()) && !mandatory.contains(cap2.getResource())) {
                c = -1;
            } else if (!mandatory.contains(cap1.getResource()) && mandatory.contains(cap2.getResource())) {
                c = 1;
            }
        }
        // Compare revision capabilities.
        if ((c == 0) && cap1.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE)) {
            c = ((Comparable<Object>) cap1.getAttributes().get(BundleNamespace.BUNDLE_NAMESPACE))
                    .compareTo(cap2.getAttributes().get(BundleNamespace.BUNDLE_NAMESPACE));
            if (c == 0) {
                Version v1 = (!cap1.getAttributes().containsKey(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE))
                        ? Version.emptyVersion
                        : (Version) cap1.getAttributes().get(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
                Version v2 = (!cap2.getAttributes().containsKey(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE))
                        ? Version.emptyVersion
                        : (Version) cap2.getAttributes().get(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
                // Compare these in reverse order, since we want
                // highest version to have priority.
                c = compareVersions(v2, v1);
            }
        // Compare package capabilities.
        } else if ((c == 0) && cap1.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE)) {
            c = ((Comparable<Object>) cap1.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))
                    .compareTo(cap2.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
            if (c == 0) {
                Version v1 = (!cap1.getAttributes().containsKey(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE))
                        ? Version.emptyVersion
                        : (Version) cap1.getAttributes().get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
                Version v2 = (!cap2.getAttributes().containsKey(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE))
                        ? Version.emptyVersion
                        : (Version) cap2.getAttributes().get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
                // Compare these in reverse order, since we want
                // highest version to have priority.
                c = compareVersions(v2, v1);
                // if same version, rather compare on the bundle version
                if (c == 0) {
                    v1 = (!cap1.getAttributes().containsKey(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE))
                            ? Version.emptyVersion
                            : (Version) cap1.getAttributes().get(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
                    v2 = (!cap2.getAttributes().containsKey(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE))
                            ? Version.emptyVersion
                            : (Version) cap2.getAttributes().get(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
                    // Compare these in reverse order, since we want
                    // highest version to have priority.
                    c = compareVersions(v2, v1);
                }
            }
        // Compare feature capabilities
        } else if ((c == 0) && cap1.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE)) {
            c = ((Comparable<Object>) cap1.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE))
                    .compareTo(cap2.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
            if (c == 0) {
                Version v1 = (!cap1.getAttributes().containsKey(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE))
                        ? Version.emptyVersion
                        : (Version) cap1.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
                Version v2 = (!cap2.getAttributes().containsKey(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE))
                        ? Version.emptyVersion
                        : (Version) cap2.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
                // Compare these in reverse order, since we want
                // highest version to have priority.
                c = compareVersions(v2, v1);
            }
        } else if (c == 0) {
            // Always prefer system bundle
            if (cap1 instanceof BundleCapability && !(cap2 instanceof BundleCapability)) {
                c = -1;
            } else if (!(cap1 instanceof BundleCapability) && cap2 instanceof BundleCapability) {
                c = 1;
            }
        }
        return c;
    }

    private int compareVersions(Version v1, Version v2) {
        int c = v1.getMajor() - v2.getMajor();
        if (c != 0) {
            return c;
        }
        c = v1.getMinor() - v2.getMinor();
        if (c != 0) {
            return c;
        }
        c = v1.getMicro() - v2.getMicro();
        if (c != 0) {
            return c;
        }
        return v1.getQualifier().compareTo(v2.getQualifier());
    }

}
