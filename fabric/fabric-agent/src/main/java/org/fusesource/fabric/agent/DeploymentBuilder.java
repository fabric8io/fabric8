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
package org.fusesource.fabric.agent;

import aQute.lib.osgi.Macro;
import aQute.lib.osgi.Processor;
import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.fusesource.common.util.Manifests;
import org.fusesource.fabric.agent.download.DownloadManager;
import org.fusesource.fabric.agent.resolver.FeatureNamespace;
import org.fusesource.fabric.agent.resolver.FeatureResource;
import org.fusesource.fabric.agent.resolver.RequirementImpl;
import org.fusesource.fabric.agent.resolver.ResolveContextImpl;
import org.fusesource.fabric.agent.resolver.ResourceBuilder;
import org.fusesource.fabric.agent.resolver.ResourceImpl;
import org.fusesource.fabric.agent.resolver.Slf4jResolverLog;
import org.fusesource.fabric.agent.utils.AgentUtils;
import org.fusesource.fabric.agent.utils.MultiException;
import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.osgi.FabBundleInfo;
import org.fusesource.fabric.fab.osgi.FabResolver;
import org.fusesource.fabric.fab.osgi.FabResolverFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.apache.felix.resolver.Util.getSymbolicName;
import static org.apache.felix.resolver.Util.getVersion;
import static org.fusesource.fabric.agent.resolver.UriNamespace.getUri;
import static org.fusesource.fabric.agent.utils.AgentUtils.FAB_PROTOCOL;
import static org.fusesource.fabric.agent.utils.AgentUtils.REQ_PROTOCOL;
import static org.fusesource.fabric.utils.PatchUtils.extractUrl;
import static org.fusesource.fabric.utils.PatchUtils.extractVersionRange;

/**
 */
public class DeploymentBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentBuilder.class);

    private final DownloadManager manager;
    private final FabResolverFactory fabResolverFactory;
    private final Collection<Repository> repositories;

    String featureRange = "${version;==}";

    AgentUtils.FileDownloader downloader;
    ResourceImpl requirements;
    Map<String, Resource> resources;
    Map<String, StreamProvider> providers;

    Set<Feature> featuresToRegister = new HashSet<Feature>();

    public DeploymentBuilder(DownloadManager manager,
                             FabResolverFactory fabResolverFactory,
                             Collection<Repository> repositories) {
        this.manager = manager;
        this.fabResolverFactory = fabResolverFactory;
        this.repositories = repositories;
    }

    public Map<String, StreamProvider> getProviders() {
        return providers;
    }

    public void download(Set<String> features,
                         Set<String> bundles,
                         Set<String> fabs,
                         Set<String> reqs,
                         Set<String> overrides,
                         Set<String> optionals) throws IOException, MultiException, InterruptedException, ResolutionException {
        this.downloader = new AgentUtils.FileDownloader(manager);
        this.resources = new ConcurrentHashMap<String, Resource>();
        this.providers = new ConcurrentHashMap<String, StreamProvider>();
        this.requirements = new ResourceImpl("dummy", "dummy", Version.emptyVersion);
        // First, gather all bundle resources
        for (String feature : features) {
            registerMatchingFeatures(feature);
        }
        for (String bundle : bundles) {
            downloadAndBuildResource(bundle);
        }
        for (String fab : fabs) {
            downloadAndBuildResource(FAB_PROTOCOL + fab);
        }
        for (String req : reqs) {
            downloadAndBuildResource(REQ_PROTOCOL + req);
        }
        for (String override : overrides) {
            // TODO: ignore download failures for overrides
            downloadAndBuildResource(extractUrl(override));
        }
        for (String optional : optionals) {
            downloadAndBuildResource(optional);
        }
        // Wait for all resources to be created
        downloader.await();
        // Do override replacement
        for (String override : overrides) {
            Resource over = resources.get(extractUrl(override));
            if (over == null) {
                // Ignore invalid overrides
                continue;
            }
            for (String uri : new ArrayList<String>(resources.keySet())) {
                Resource res = resources.get(uri);
                if (getSymbolicName(res).equals(getSymbolicName(over))) {
                    VersionRange range;
                    String vr = extractVersionRange(override);
                    if (vr == null) {
                        // default to micro version compatibility
                        Version v1 = getVersion(res);
                        Version v2 = new Version(v1.getMajor(), v1.getMinor() + 1, 0);
                        range = new VersionRange(false, v1, v2, true);
                    } else {
                        range = VersionRange.parseVersionRange(vr);
                    }
                    // The resource matches, so replace it with the overriden resource
                    if (range.contains(getVersion(res))) {
                        resources.put(uri, over);
                    }
                }
            }
        }
        // Build features resources
        for (Feature feature : featuresToRegister) {
            Resource resource = FeatureResource.build(feature, featureRange, resources);
            resources.put("feature:" + feature.getName() + "/" + feature.getVersion(), resource);
        }
        // Build requirements
        for (String feature : features) {
            requireFeature(feature, requirements);
        }
        for (String bundle : bundles) {
            requireResource(bundle);
        }
        for (String req : reqs) {
            requireResource(REQ_PROTOCOL + req);
        }
        for (String fab : fabs) {
            requireResource(FAB_PROTOCOL + fab);
        }
    }

    public Collection<Resource> resolve(Resource systemBundle,
                                        boolean resolveOptionalImports) throws ResolutionException {
        // Resolve
        resources.put("system-bundle", systemBundle);

        ResolverImpl resolver = new ResolverImpl(new Slf4jResolverLog(LOGGER));
        ResolveContext context = new ResolveContextImpl(
                Collections.<Resource>singleton(requirements),
                Collections.<Resource>emptySet(),
                resources.values(),
                resolveOptionalImports);

        Map<Resource, List<Wire>> wiring = resolver.resolve(context);
        Map<String, Resource> deploy = new TreeMap<String, Resource>();
        for (Resource res : wiring.keySet()) {
            String uri = getUri(res);
            if (uri != null) {
                deploy.put(uri, res);
            }
        }
        return deploy.values();
    }

    public void requireFeature(String feature, ResourceImpl resource) throws IOException {
        // Find name and version range
        String[] split = feature.split("/");
        String name = split[0].trim();
        String version = (split.length > 1) ? split[1].trim() : "";
        VersionRange range = version.length() == 0
                        ? VersionRange.ANY_VERSION : new VersionRange(version);
        // Add requirement
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(IdentityNamespace.IDENTITY_NAMESPACE, name);
        attrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, FeatureNamespace.TYPE_FEATURE);
        attrs.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, range);
        resource.addRequirement(
                new RequirementImpl(resource, IdentityNamespace.IDENTITY_NAMESPACE,
                        Collections.<String, String>emptyMap(), attrs));
    }

    public void requireResource(String location) {
        Resource res = resources.get(location);
        if (res == null) {
            throw new IllegalStateException("Could not find resource for " + location);
        }
        List<Capability> caps = res.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
        if (caps.size() != 1) {
            throw new IllegalStateException("Resource does not have a single " + IdentityNamespace.IDENTITY_NAMESPACE + " capability");
        }
        Capability cap = caps.get(0);
        // Add requirement
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(IdentityNamespace.IDENTITY_NAMESPACE, cap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
        attrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, cap.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
        attrs.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, new VersionRange((Version) cap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE), true));
        requirements.addRequirement(
                new RequirementImpl(requirements, IdentityNamespace.IDENTITY_NAMESPACE,
                        Collections.<String, String>emptyMap(), attrs));

    }

    public void registerMatchingFeatures(String feature) throws IOException {
        // Find name and version range
        String[] split = feature.split("/");
        String name = split[0].trim();
        String version = (split.length > 1)
                ? split[1].trim() : Version.emptyVersion.toString();
        // Register matching features
        registerMatchingFeatures(name, new VersionRange(version));
    }

    public void registerMatchingFeatures(Feature feature) throws IOException {
        registerMatchingFeatures(feature.getName(), feature.getVersion());
    }

    public void registerMatchingFeatures(String name, String version) throws IOException {
        if (!version.startsWith("[") && !version.startsWith("(")) {
            Processor processor = new Processor();
            processor.setProperty("@", VersionTable.getVersion(version).toString());
            Macro macro = new Macro(processor);
            version = macro.process(featureRange);
        }
        registerMatchingFeatures(name, new VersionRange(version));
    }

    public void registerMatchingFeatures(String name, VersionRange range) throws IOException {
        for (Repository repo : repositories) {
            Feature[] features;
            try {
                features = repo.getFeatures();
            } catch (Exception e) {
                // This should not happen as the repository has been loaded already
                throw new IllegalStateException(e);
            }
            for (Feature f : features) {
                if (name.equals(f.getName())) {
                    Version v = VersionTable.getVersion(f.getVersion());
                    if (range.contains(v)) {
                        featuresToRegister.add(f);
                        for (Feature dep : f.getDependencies()) {
                            registerMatchingFeatures(dep);
                        }
                        for (BundleInfo bundle : f.getBundles()) {
                            downloadAndBuildResource(bundle.getLocation());
                        }
                    }
                }
            }
        }
    }

    public void downloadAndBuildResource(final String location) throws IOException {
        if (location.startsWith(FAB_PROTOCOL)) {
            String url = location.substring(FAB_PROTOCOL.length());
            downloader.download(url, new AgentUtils.DownloadCallback() {
                @Override
                public void downloaded(File file) throws Exception {
                    FabResolver resolver = fabResolverFactory.getResolver(file.toURI().toURL());
                    FabBundleInfo fabInfo = resolver.getInfo();

                    ResourceImpl resource = (ResourceImpl) manageResource(location, fabInfo.getManifest(), new StreamProvider.Fab(fabInfo));
                    for (String name : fabInfo.getFeatures()) {
                        registerMatchingFeatures(name);
                        requireFeature(name, resource);
                    }
                    for (DependencyTree dep : fabInfo.getBundles()) {
                        File depFile = dep.getJarFile();
                        Attributes attrs = getAttributes(dep.getJarURL().toString(), depFile);
                        if (attrs.getValue(Constants.BUNDLE_SYMBOLICNAME) != null) {
                            manageResource(getMvnUrl(dep), attrs, new StreamProvider.File(depFile));
                        }
                    }
                }
            });
        } else if (location.startsWith(REQ_PROTOCOL)) {
            try {
                ResourceImpl resource = new ResourceImpl(location, "dummy", Version.emptyVersion);
                for (Requirement req : ResourceBuilder.parseRequirement(resource, location.substring(REQ_PROTOCOL.length()))) {
                    resource.addRequirement(req);
                }
                resources.put(location, resource);
            } catch (BundleException e) {
                throw new IOException("Error parsing requirement", e);
            }
        } else {
            downloader.download(location, new AgentUtils.DownloadCallback() {
                @Override
                public void downloaded(File file) throws Exception {
                    manageResource(location, file);
                }
            });
        }
    }

    private String getMvnUrl(DependencyTree dep) {
        String groupId = dep.getGroupId();
        String artifactId = dep.getArtifactId();
        String version = dep.getVersion();
        String classifier = dep.getClassifier();
        String extension = dep.getExtension();
        StringBuilder sb = new StringBuilder();
        sb.append("mvn:");
        sb.append(groupId);
        sb.append("/");
        sb.append(artifactId);
        sb.append("/");
        sb.append(version);
        if (!"".equals(classifier) || !"jar".equals(extension)) {
            sb.append("/");
            sb.append(extension);
        }
        if (!"".equals(classifier)) {
            sb.append("/");
            sb.append(classifier);
        }
        return sb.toString();
    }

    private Resource manageResource(String location, File file) throws Exception {
        Resource resource = resources.get(location);
        if (resource == null) {
            Attributes attributes = getAttributes(location, file);
            resource = manageResource(location, attributes, new StreamProvider.File(file));
        }
        return resource;
    }

    private Resource manageResource(String location, Attributes attributes, StreamProvider provider) throws Exception {
        Resource resource = resources.get(location);
        if (resource == null) {
            resource = createResource(location, attributes);
            resources.put(location, resource);
            providers.put(location, provider);
        }
        return resource;
    }

    private Resource createResource(String uri, Attributes attributes) throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        for (Map.Entry attr : attributes.entrySet()) {
            headers.put(attr.getKey().toString(), attr.getValue().toString());
        }
        try {
            return ResourceBuilder.build(uri, headers);
        } catch (BundleException e) {
            throw new Exception("Unable to create resource for bundle " + uri, e);
        }
    }

    protected Attributes getAttributes(String uri, File file) throws Exception {
        Manifest man = Manifests.getManifest(file);
        if (man == null) {
            throw new IllegalArgumentException("Resource " + uri + " does not contain a manifest");
        }
        return man.getMainAttributes();
    }

}
