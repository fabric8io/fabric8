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
package org.fusesource.fabric.agent.resolver;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
*/
public class FeatureResource extends ResourceImpl {

    public static Resource build(Feature feature, Map<String, Resource> locToRes) {
        FeatureResource resource = new FeatureResource(feature.getName(), VersionTable.getVersion(feature.getVersion()));
        Map<String, String> dirs = new HashMap<String, String>();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(FeatureNamespace.FEATURE_NAMESPACE, feature.getName());
        attrs.put(FeatureNamespace.CAPABILITY_VERSION_ATTRIBUTE, VersionTable.getVersion(feature.getVersion()));
        resource.addCapability(new CapabilityImpl(resource, FeatureNamespace.FEATURE_NAMESPACE, dirs, attrs));
        for (BundleInfo info : feature.getBundles()) {
            if (!info.isDependency()) {
                Resource res = locToRes.get(info.getLocation());
                if (res == null) {
                    throw new IllegalStateException("Resource not found for url " + info.getLocation());
                }
                List<Capability> caps = res.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
                if (caps.size() != 1) {
                    throw new IllegalStateException("Resource does not have a single " + IdentityNamespace.IDENTITY_NAMESPACE + " capability");
                }
                dirs = new HashMap<String, String>();
                attrs = new HashMap<String, Object>();
                attrs.put(IdentityNamespace.IDENTITY_NAMESPACE, caps.get(0).getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
                attrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, caps.get(0).getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
                attrs.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, new VersionRange((Version) caps.get(0).getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE), true));
                resource.addRequirement(new RequirementImpl(resource, IdentityNamespace.IDENTITY_NAMESPACE, dirs, attrs));
            }
        }
        for (Feature dep : feature.getDependencies()) {
            String name = dep.getName();
            String version = dep.getVersion();
            dirs = new HashMap<String, String>();
            attrs = new HashMap<String, Object>();
            attrs.put(FeatureNamespace.FEATURE_NAMESPACE, name);
            attrs.put(FeatureNamespace.CAPABILITY_VERSION_ATTRIBUTE, new VersionRange(version));
            resource.addRequirement(new RequirementImpl(resource, FeatureNamespace.FEATURE_NAMESPACE, dirs, attrs));
        }
        return resource;
    }

    public FeatureResource(String name, Version version) {
        super(name, FeatureNamespace.TYPE_FEATURE, version);
    }
}
