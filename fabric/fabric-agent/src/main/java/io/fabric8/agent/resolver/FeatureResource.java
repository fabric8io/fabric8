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
package io.fabric8.agent.resolver;

import java.util.List;
import java.util.Map;

import io.fabric8.agent.internal.Macro;
import io.fabric8.agent.model.BundleInfo;
import io.fabric8.agent.model.Capability;
import io.fabric8.agent.model.Conditional;
import io.fabric8.agent.model.Dependency;
import io.fabric8.agent.model.Feature;
import io.fabric8.agent.model.Requirement;
import org.apache.felix.utils.version.VersionTable;
import org.osgi.framework.BundleException;
import org.osgi.resource.Resource;

import static io.fabric8.agent.resolver.ResourceUtils.TYPE_FEATURE;
import static io.fabric8.agent.resolver.ResourceUtils.addIdentityRequirement;

/**
 */
public final class FeatureResource extends ResourceImpl {

    private final Feature feature;

    private FeatureResource(Feature feature) {
        super(feature.getName(), TYPE_FEATURE, VersionTable.getVersion(feature.getVersion()));
        this.feature = feature;
    }

    public static FeatureResource build(Feature feature, Conditional conditional, String featureRange, Map<String, ? extends Resource> locToRes) throws BundleException {
        Feature fcond = conditional.asFeature(feature.getName(), feature.getVersion());
        FeatureResource resource = build(fcond, featureRange, locToRes);
        for (String cond : conditional.getCondition()) {
            if (cond.startsWith("req:")) {
                cond = cond.substring("req:".length());
                List<org.osgi.resource.Requirement> reqs = ResourceBuilder.parseRequirement(resource, cond);
                resource.addRequirements(reqs);
            } else {
                Dependency dep = new Dependency();
                String[] p = cond.split("/");
                dep.setName(p[0]);
                if (p.length > 1) {
                    dep.setVersion(p[1]);
                }
                addDependency(resource, dep, featureRange);
            }
        }
        Dependency dep = new Dependency();
        dep.setName(feature.getName());
        dep.setVersion(feature.getVersion());
        addDependency(resource, dep, featureRange);
        return resource;
    }

    public static FeatureResource build(Feature feature, String featureRange, Map<String, ? extends Resource> locToRes) throws BundleException {
        FeatureResource resource = new FeatureResource(feature);
        for (BundleInfo info : feature.getBundles()) {
            if (!info.isDependency()) {
                Resource res = locToRes.get(info.getLocation());
                if (res == null) {
                    throw new IllegalStateException("Resource not found for url " + info.getLocation());
                }
                addIdentityRequirement(resource, res);
            }
        }
        for (Dependency dep : feature.getDependencies()) {
            if (!dep.isDependency()) {
                addDependency(resource, dep, featureRange);
            }
        }
        for (Capability cap : feature.getCapabilities()) {
            resource.addCapabilities(ResourceBuilder.parseCapability(resource, cap.getValue()));
        }
        for (Requirement req : feature.getRequirements()) {
            resource.addRequirements(ResourceBuilder.parseRequirement(resource, req.getValue()));
        }
        return resource;
    }

    protected static void addDependency(FeatureResource resource, Dependency dep, String featureRange) {
        String name = dep.getName();
        String version = dep.getVersion();
        if (version.equals("0.0.0")) {
            version = null;
        } else if (!version.startsWith("[") && !version.startsWith("(")) {
            version = Macro.transform(featureRange, version);
        }
        addIdentityRequirement(resource, name, TYPE_FEATURE, version);
    }

    public Feature getFeature() {
        return feature;
    }
}
