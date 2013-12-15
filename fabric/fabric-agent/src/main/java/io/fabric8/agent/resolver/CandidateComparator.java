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
package io.fabric8.agent.resolver;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.resource.Capability;

import java.util.Comparator;

public class CandidateComparator implements Comparator<Capability>
{
    public int compare(Capability cap1, Capability cap2)
    {
        int c = 0;
        // Always prefer system bundle
        if (cap1 instanceof BundleCapability && !(cap2 instanceof BundleCapability)) {
            c = -1;
        } else if (!(cap1 instanceof BundleCapability) && cap2 instanceof BundleCapability) {
            c = 1;
        }
        // Compare revision capabilities.
        if ((c == 0) && cap1.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE))
        {
            c = ((Comparable) cap1.getAttributes().get(BundleNamespace.BUNDLE_NAMESPACE))
                    .compareTo(cap2.getAttributes().get(BundleNamespace.BUNDLE_NAMESPACE));
            if (c == 0)
            {
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
        }
        // Compare package capabilities.
        else if ((c == 0) && cap1.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
        {
            c = ((Comparable) cap1.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))
                    .compareTo(cap2.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
            if (c == 0)
            {
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
                if (c == 0)
                {
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
        }
        // Compare feature capabilities
        else if ((c == 0) && cap1.getNamespace().equals(FeatureNamespace.FEATURE_NAMESPACE))
        {
            c = ((Comparable) cap1.getAttributes().get(FeatureNamespace.FEATURE_NAMESPACE))
                    .compareTo(cap2.getAttributes().get(FeatureNamespace.FEATURE_NAMESPACE));
            if (c == 0)
            {
                Version v1 = (!cap1.getAttributes().containsKey(FeatureNamespace.CAPABILITY_VERSION_ATTRIBUTE))
                        ? Version.emptyVersion
                        : (Version) cap1.getAttributes().get(FeatureNamespace.CAPABILITY_VERSION_ATTRIBUTE);
                Version v2 = (!cap2.getAttributes().containsKey(FeatureNamespace.CAPABILITY_VERSION_ATTRIBUTE))
                        ? Version.emptyVersion
                        : (Version) cap2.getAttributes().get(FeatureNamespace.CAPABILITY_VERSION_ATTRIBUTE);
                // Compare these in reverse order, since we want
                // highest version to have priority.
                c = compareVersions(v2, v1);
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
        String q1 = cleanQualifierForComparison(v1.getQualifier());
        String q2 = cleanQualifierForComparison(v2.getQualifier());
        return q1.compareTo(q2);
    }

    private String cleanQualifierForComparison(String qualifier) {
        return qualifier.replaceAll("(redhat-[0-9]{3})([0-9]{3})", "$1-$2");
    }
}