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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.fabric8.agent.download.DownloadCallback;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.Downloader;
import io.fabric8.agent.download.StreamProvider;
import io.fabric8.agent.model.BundleInfo;
import io.fabric8.agent.model.Conditional;
import io.fabric8.agent.model.Dependency;
import io.fabric8.agent.model.Feature;
import io.fabric8.agent.model.ScopeFilter;
import io.fabric8.agent.internal.Overrides;
import io.fabric8.agent.service.MetadataBuilder;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import io.fabric8.agent.resolver.FeatureResource;
import io.fabric8.agent.resolver.ResourceBuilder;
import io.fabric8.agent.resolver.ResourceImpl;
import io.fabric8.agent.resolver.ResourceUtils;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import static io.fabric8.agent.resolver.ResourceUtils.TYPE_FEATURE;
import static io.fabric8.agent.resolver.ResourceUtils.TYPE_SUBSYSTEM;
import static io.fabric8.agent.resolver.ResourceUtils.addIdentityRequirement;
import static io.fabric8.agent.resolver.ResourceUtils.getSymbolicName;
import static io.fabric8.agent.resolver.ResourceUtils.getUri;
import static io.fabric8.agent.resolver.ResourceUtils.getVersion;
import static io.fabric8.agent.resolver.ResourceUtils.toFeatureRequirement;
import static io.fabric8.agent.internal.MapUtils.addToMapSet;
import static org.eclipse.equinox.region.RegionFilter.VISIBLE_ALL_NAMESPACE;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;

public class Subsystem extends ResourceImpl {

    private static final String ALL_FILTER = "(|(!(all=*))(all=*))";

    private static final String SUBSYSTEM_FILTER = String.format("(%s=%s)", CAPABILITY_TYPE_ATTRIBUTE, TYPE_SUBSYSTEM);

    private static final String FEATURE_FILTER = String.format("(%s=%s)", CAPABILITY_TYPE_ATTRIBUTE, TYPE_FEATURE);

    private static final String SUBSYSTEM_OR_FEATURE_FILTER = String.format("(|%s%s)", SUBSYSTEM_FILTER, FEATURE_FILTER);

    // Everything is visible
    private static final Map<String, Set<String>> SHARE_ALL_POLICY =
            Collections.singletonMap(
                    VISIBLE_ALL_NAMESPACE,
                    Collections.singleton(ALL_FILTER));

    // Nothing (but systems) is visible
    private static final Map<String, Set<String>> SHARE_NONE_POLICY =
            Collections.singletonMap(
                    IDENTITY_NAMESPACE,
                    Collections.singleton(SUBSYSTEM_FILTER));

    private final String name;
    private final boolean acceptDependencies;
    private final Subsystem parent;
    private final Feature feature;
    private final List<Subsystem> children = new ArrayList<>();
    private final Map<String, Set<String>> importPolicy;
    private final Map<String, Set<String>> exportPolicy;
    private final List<Resource> installable = new ArrayList<>();
    private final Map<String, DependencyInfo> dependencies = new HashMap<>();
    private final List<Requirement> dependentFeatures = new ArrayList<>();

    private final List<String> bundles = new ArrayList<>();

    public Subsystem(String name) {
        super(name, TYPE_SUBSYSTEM, Version.emptyVersion);
        this.name = name;
        this.parent = null;
        this.acceptDependencies = true;
        this.feature = null;
        this.importPolicy = SHARE_NONE_POLICY;
        this.exportPolicy = SHARE_NONE_POLICY;
    }

    public Subsystem(String name, Feature feature, Subsystem parent) {
        super(name, TYPE_SUBSYSTEM, Version.emptyVersion);
        this.name = name;
        this.parent = parent;
        this.acceptDependencies = feature.getScoping() != null && feature.getScoping().acceptDependencies();
        this.feature = feature;
        if (feature.getScoping() != null) {
            this.importPolicy = createPolicy(feature.getScoping().getImports());
            this.importPolicy.put(IDENTITY_NAMESPACE, Collections.singleton(SUBSYSTEM_OR_FEATURE_FILTER));
            this.exportPolicy = createPolicy(feature.getScoping().getExports());
            this.exportPolicy.put(IDENTITY_NAMESPACE, Collections.singleton(SUBSYSTEM_OR_FEATURE_FILTER));
        } else {
            this.importPolicy = SHARE_ALL_POLICY;
            this.exportPolicy = SHARE_ALL_POLICY;
        }

        addIdentityRequirement(this,
                feature.getName(),
                TYPE_FEATURE,
                new VersionRange(VersionTable.getVersion(feature.getVersion()), true));
    }

    public Subsystem(String name, Subsystem parent, boolean acceptDependencies) {
        super(name, TYPE_SUBSYSTEM, Version.emptyVersion);
        this.name = name;
        this.parent = parent;
        this.acceptDependencies = acceptDependencies;
        this.feature = null;
        this.importPolicy = SHARE_ALL_POLICY;
        this.exportPolicy = SHARE_NONE_POLICY;
    }

    public List<Resource> getInstallable() {
        return installable;
    }

    public String getName() {
        return name;
    }

    public Subsystem getParent() {
        return parent;
    }

    public Collection<Subsystem> getChildren() {
        return children;
    }

    public Subsystem getChild(String name) {
        for (Subsystem child : children) {
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    public boolean isAcceptDependencies() {
        return acceptDependencies;
    }

    public Map<String, Set<String>> getImportPolicy() {
        return importPolicy;
    }

    public Map<String, Set<String>> getExportPolicy() {
        return exportPolicy;
    }

    public Feature getFeature() {
        return feature;
    }

    public Subsystem createSubsystem(String name, boolean acceptDependencies) {
        if (feature != null) {
            throw new UnsupportedOperationException("Can not create application subsystems inside a feature subsystem");
        }
        // Create subsystem
        String childName = getName() + "/" + name;
        Subsystem as = new Subsystem(childName, this, acceptDependencies);
        children.add(as);
        // Add a requirement to force its resolution
        ResourceUtils.addIdentityRequirement(this, childName, TYPE_SUBSYSTEM, (VersionRange) null);
        // Add it to repo
        installable.add(as);
        return as;
    }

    public void addSystemResource(Resource resource) {
        installable.add(resource);
    }

    public void requireFeature(String name, String range, boolean mandatory) {
        if (mandatory) {
            ResourceUtils.addIdentityRequirement(this, name, TYPE_FEATURE, range);
        } else {
            ResourceImpl res = new ResourceImpl();
            ResourceUtils.addIdentityRequirement(res, name, TYPE_FEATURE, range);
            dependentFeatures.addAll(res.getRequirements(null));
        }
    }

    public void require(String requirement) throws BundleException {
        int idx = requirement.indexOf(":");
        String type, req;
        if (idx >= 0) {
            type = requirement.substring(0, idx);
            req = requirement.substring(idx + 1);
        } else {
            type = "feature";
            req = requirement;
        }
        switch (type) {
        case "feature":
            addRequirement(toFeatureRequirement(req));
            break;
        case "requirement":
            addRequirement(req);
            break;
        case "bundle":
            bundles.add(req);
            break;
        }
    }

    protected void addRequirement(String requirement) throws BundleException {
        for (Requirement req : ResourceBuilder.parseRequirement(this, requirement)) {
            Object range = req.getAttributes().get(CAPABILITY_VERSION_ATTRIBUTE);
            if (range instanceof String) {
                req.getAttributes().put(CAPABILITY_VERSION_ATTRIBUTE, new VersionRange((String) range));
            }
            addRequirement(req);
        }
    }

    public Map<String, BundleInfo> getBundleInfos() {
        Map<String, BundleInfo> infos = new HashMap<>();
        for (DependencyInfo di : dependencies.values()) {
            infos.put(di.getLocation(), di);
        }
        return infos;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void build(Collection<Feature> features) throws Exception {
        for (Subsystem child : children) {
            child.build(features);
        }
        if (feature != null) {
            for (Dependency dep : feature.getDependencies()) {
                Subsystem ss = this;
                while (!ss.isAcceptDependencies()) {
                    ss = ss.getParent();
                }
                ss.requireFeature(dep.getName(), dep.getVersion(), !dep.isDependency());
            }
        }
        List<Requirement> processed = new ArrayList<>();
        while (true) {
            List<Requirement> requirements = getRequirements(IDENTITY_NAMESPACE);
            requirements.addAll(dependentFeatures);
            requirements.removeAll(processed);
            if (requirements.isEmpty()) {
                break;
            }
            for (Requirement requirement : requirements) {
                String name = (String) requirement.getAttributes().get(IDENTITY_NAMESPACE);
                String type = (String) requirement.getAttributes().get(CAPABILITY_TYPE_ATTRIBUTE);
                VersionRange range = (VersionRange) requirement.getAttributes().get(CAPABILITY_VERSION_ATTRIBUTE);
                if (TYPE_FEATURE.equals(type)) {
                    for (Feature feature : features) {
                        if (feature.getName().equals(name)
                                && (range == null || range.contains(VersionTable.getVersion(feature.getVersion())))) {
                            if (feature != this.feature) {
                                String ssName = this.name + "#" + (feature.hasVersion() ? feature.getName() + "-" + feature.getVersion() : feature.getName());
                                Subsystem fs = getChild(ssName);
                                if (fs == null) {
                                    fs = new Subsystem(ssName, feature, this);
                                    fs.build(features);
                                    installable.add(fs);
                                    children.add(fs);
                                }
                            }
                        }
                    }
                }
                processed.add(requirement);
            }
        }
    }

    public Set<String> collectPrerequisites() {
        Set<String> prereqs = new HashSet<>();
        doCollectPrerequisites(prereqs);
        return prereqs;
    }

    private void doCollectPrerequisites(Set<String> prereqs) {
        for (Subsystem child : children) {
            child.doCollectPrerequisites(prereqs);
        }
        if (feature != null) {
            for (Dependency dep : feature.getDependencies()) {
                if (dep.isPrerequisite()) {
                    prereqs.add(dep.toString());
                }
            }
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void downloadBundles(DownloadManager manager,
                                MetadataBuilder builder,
                                Set<String> overrides,
                                String featureResolutionRange) throws Exception {
        for (Subsystem child : children) {
            child.downloadBundles(manager, builder, overrides, featureResolutionRange);
        }
        final Map<String, ResourceImpl> bundles = new ConcurrentHashMap<>();
        final Downloader downloader = manager.createDownloader();
        final Map<BundleInfo, Conditional> infos = new HashMap<>();
        if (feature != null) {
            for (Conditional cond : feature.getConditional()) {
                for (final BundleInfo bi : cond.getBundles()) {
                    infos.put(bi, cond);
                }
            }
            for (BundleInfo bi : feature.getBundles()) {
                infos.put(bi, null);
            }
        }
        ResourceBuilderCallback callback = new ResourceBuilderCallback(bundles, builder);
        for (Map.Entry<BundleInfo, Conditional> entry : infos.entrySet()) {
            final BundleInfo bi = entry.getKey();
            final String loc = bi.getLocation();
            downloader.download(loc, callback);
        }
        for (Clause bundle : Parser.parseClauses(this.bundles.toArray(new String[this.bundles.size()]))) {
            final String loc = bundle.getName();
            downloader.download(loc, callback);
        }
        for (String override : overrides) {
            final String loc = Overrides.extractUrl(override);
            downloader.download(loc, callback);
        }
        downloader.await();
        Overrides.override(bundles, overrides);
        if (feature != null) {
            // Add conditionals
            Map<Conditional, Resource> resConds = new HashMap<>();
            for (Conditional cond : feature.getConditional()) {
                FeatureResource resCond = FeatureResource.build(feature, cond, featureResolutionRange, bundles);
                addIdentityRequirement(this, resCond, false);
                addIdentityRequirement(resCond, this, true);
                installable.add(resCond);
                resConds.put(cond, resCond);
            }
            // Add features
            FeatureResource resFeature = FeatureResource.build(feature, featureResolutionRange, bundles);
            addIdentityRequirement(resFeature, this);
            installable.add(resFeature);
            // Add dependencies
            for (Map.Entry<BundleInfo, Conditional> entry : infos.entrySet()) {
                final BundleInfo bi = entry.getKey();
                final String loc = bi.getLocation();
                final Conditional cond = entry.getValue();
                ResourceImpl res = bundles.get(loc);
                if (bi.isDependency()) {
                    addDependency(res, false, bi.isStart(), bi.getStartLevel());
                } else {
                    doAddDependency(res, cond == null, bi.isStart(), bi.getStartLevel());
                }
                if (cond != null) {
                    addIdentityRequirement(res, resConds.get(cond), true);
                }
            }
        }
        for (Clause bundle : Parser.parseClauses(this.bundles.toArray(new String[this.bundles.size()]))) {
            final String loc = bundle.getName();
            boolean dependency = Boolean.parseBoolean(bundle.getAttribute("dependency"));
            boolean start = bundle.getAttribute("start") == null || Boolean.parseBoolean(bundle.getAttribute("start"));
            int startLevel = 0;
            try {
                startLevel = Integer.parseInt(bundle.getAttribute("start-level"));
            } catch (NumberFormatException e) {
                // Ignore
            }
            if (dependency) {
                addDependency(bundles.get(loc), false, start, startLevel);
            } else {
                doAddDependency(bundles.get(loc), true, start, startLevel);
                addIdentityRequirement(this, bundles.get(loc));
            }
        }
        // Compute dependencies
        for (DependencyInfo info : dependencies.values()) {
            installable.add(info.resource);
            addIdentityRequirement(info.resource, this, info.mandatory);
        }
    }

    class ResourceBuilderCallback implements DownloadCallback {
        final Map<String, ResourceImpl> bundles;
        final MetadataBuilder builder;
        ResourceBuilderCallback(Map<String, ResourceImpl> bundles, MetadataBuilder builder) {
            this.bundles = bundles;
            this.builder = builder;
        }
        @Override
        public void downloaded(StreamProvider provider) throws Exception {
            String loc = provider.getUrl();
            Map<String, String> headers = builder.getMetadata(provider.getUrl(), provider.getFile());
            ResourceImpl res = createResource(loc, headers);
            bundles.put(loc, res);
        }
    }

    void addDependency(ResourceImpl resource, boolean mandatory, boolean start, int startLevel) {
        if (isAcceptDependencies()) {
            doAddDependency(resource, mandatory, start, startLevel);
        } else {
            parent.addDependency(resource, mandatory, start, startLevel);
        }
    }

    private void doAddDependency(ResourceImpl resource, boolean mandatory, boolean start, int startLevel) {
        String id = getSymbolicName(resource) + "|" + getVersion(resource);
        DependencyInfo info = dependencies.get(id);
        if (info == null) {
            info = new DependencyInfo();
            dependencies.put(id, info);
        }
        info.resource = resource;
        info.mandatory |= mandatory;
        info.start |= start;
        if (info.startLevel > 0 && startLevel > 0) {
            info.startLevel = Math.min(info.startLevel, startLevel);
        } else {
            info.startLevel = Math.max(info.startLevel, startLevel);
        }
    }

    class DependencyInfo extends BundleInfo {
        ResourceImpl resource;
        boolean mandatory;
        boolean start;
        int startLevel;

        @Override
        public boolean isStart() {
            return start;
        }

        @Override
        public int getStartLevel() {
            return startLevel;
        }

        @Override
        public String getLocation() {
            return getUri(resource);
        }

        @Override
        public boolean isDependency() {
            return !mandatory;
        }
    }

    Map<String, Set<String>> createPolicy(List<? extends ScopeFilter> filters) {
        Map<String, Set<String>> policy = new HashMap<>();
        for (ScopeFilter filter : filters) {
            addToMapSet(policy, filter.getNamespace(), filter.getFilter());
        }
        return policy;
    }

    ResourceImpl createResource(String uri, Map<String, String> headers) throws Exception {
        try {
            return ResourceBuilder.build(uri, headers);
        } catch (BundleException e) {
            throw new Exception("Unable to create resource for bundle " + uri, e);
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
