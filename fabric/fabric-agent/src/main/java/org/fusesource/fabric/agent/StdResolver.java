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

import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.fusesource.fabric.agent.resolver.FeatureResource;
import org.fusesource.fabric.agent.resolver.ResolveContextImpl;
import org.fusesource.fabric.agent.resolver.ResourceBuilder;
import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.osgi.FabBundleInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.felix.resolver.Util.getSymbolicName;
import static org.apache.felix.resolver.Util.getVersion;
import static org.fusesource.fabric.agent.resolver.UriNamespace.getUri;
import static org.fusesource.fabric.agent.utils.AgentUtils.FAB_PROTOCOL;
import static org.fusesource.fabric.utils.PatchUtils.extractUrl;
import static org.fusesource.fabric.utils.PatchUtils.extractVersionRange;

public class StdResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdResolver.class);

    private boolean resolveOptionalImports;

    private Map<String, Resource> resources = new TreeMap<String, Resource>();
    private Set<Resource> mandatory = new HashSet<Resource>();

    public StdResolver() {
    }

    public StdResolver(boolean resolveOptionalImports) {
        this.resolveOptionalImports = resolveOptionalImports;
    }

    public boolean isResolveOptionalImports() {
        return resolveOptionalImports;
    }

    /**
     * When set to <code>true</code>, the OBR resolver will try to resolve optional imports as well.
     * Defaults to <code>false</code>
     *
     * @param resolveOptionalImports
     */
    public void setResolveOptionalImports(boolean resolveOptionalImports) {
        this.resolveOptionalImports = resolveOptionalImports;
    }

    public Collection<Resource> resolve(Set<Feature> features,
                                  Set<String> bundles,
                                  Map<String, FabBundleInfo> fabs,
                                  Set<String> overrides,
                                  Map<String, File> downloads) throws Exception {

        for (Feature feature : features) {
            for (BundleInfo bundleInfo : feature.getBundles()) {
                //We ignore Fabs completely as the are already been added to fabs set.
                if (bundleInfo.getLocation().startsWith(FAB_PROTOCOL)) {
                    continue;
                }
                // TODO: handle custom requirements
                Attributes attributes = getAttributes(bundleInfo.getLocation(), downloads, fabs);
                Resource resource = manageResource(bundleInfo.getLocation(), attributes);
                /*
                try {
                    if (resource == null) {
                        resource = createResource(bundleInfo.getLocation(), attributes);
                        if (resource == null) {
                            throw new IllegalArgumentException("Unable to build resource for bundle " + bundleInfo.getLocation());
                        }
                        resources.put(bundleInfo.getLocation(), resource);
                    }
                } catch (MalformedURLException e) {
                    resource = parseRequirement(bundleInfo.getLocation());
                }
                if (!bundleInfo.isDependency()) {
                    mandatory.add(resource);
                }
                infos.put(resource, bundleInfo);
                */
            }
            Resource resource = FeatureResource.build(feature, resources);
            resources.put(feature.getName() + "/" + feature.getVersion(), resource);
            mandatory.add(resource);
        }
        for (String bundle : bundles) {
            Attributes attributes = getAttributes(bundle, downloads, fabs);
            Resource resource = manageResource(bundle, attributes);
            mandatory.add(resource);
        }
        for (FabBundleInfo fab : fabs.values()) {
            Attributes attributes = fab.getManifest();
            Resource resource = manageResource(FAB_PROTOCOL + fab.getUrl(), attributes);
            mandatory.add(resource);

            for (DependencyTree dep : fab.getBundles()) {
                if (dep.isBundle()) {
                    Attributes depAttributes = getAttributes(dep.getUrl(), downloads, fabs);
                    manageResource(dep.getUrl(), depAttributes);
                }
            }
        }
        for (String override : overrides) {
            Resource over = null;
            try {
                String url = extractUrl(override);
                Attributes attributes = getAttributes(url, downloads, fabs);
                over = createResource(url, attributes);
            } catch (Exception e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.info("Ignoring patched resource: {}: {}", new Object[] { override, e.getMessage() }, e);
                } else {
                    LOGGER.info("Ignoring patched resource: {}: {}", override, e.getMessage());
                }
            }
            if (over == null) {
                // Artifacts may not be valid bundles, so just ignore those artifacts
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
                        resources.remove(uri);
                        resources.put(override, over);
                        if (mandatory.remove(res)) {
                            mandatory.add(over);
                        }
                    }
                }
            }
        }

        Bundle systemBundle = FrameworkUtil.getBundle(StdResolver.class).getBundleContext().getBundle(0);
        resources.put("system-bundle", systemBundle.adapt(BundleRevision.class));

        ResolverImpl resolver = new ResolverImpl(new org.apache.felix.resolver.Logger(org.apache.felix.resolver.Logger.LOG_DEBUG) {
            @Override
            protected void doLog(int level, String msg, Throwable throwable) {
                switch (level) {
                    case LOG_ERROR:
                        LOGGER.error(msg, throwable);
                        break;
                    case LOG_WARNING:
                        LOGGER.warn(msg, throwable);
                        break;
                    case LOG_INFO:
                        LOGGER.info(msg, throwable);
                        break;
                    case LOG_DEBUG:
                        LOGGER.debug(msg, throwable);
                        break;
                }
            }
        });
        ResolveContext context = new ResolveContextImpl(mandatory, Collections.<Resource>emptySet(), resources.values(), resolveOptionalImports);

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

    private Resource manageResource(String location, Attributes attributes) throws Exception {
        Resource resource = resources.get(location);
        if (resource == null) {
            resource = createResource(location, attributes);
            if (resource == null) {
                throw new IllegalArgumentException("Unable to build resource for bundle " + location);
            }
            resources.put(location, resource);
        }
        return resource;
    }

    private boolean doResolve(org.apache.felix.bundlerepository.Resolver resolver) {
        if (resolveOptionalImports) {
            return resolver.resolve();
        } else {
            return resolver.resolve(org.apache.felix.bundlerepository.Resolver.NO_OPTIONAL_RESOURCES);
        }
    }

    private int compareFuseVersions(Version v1, Version v2) {
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
        String q1 = v1.getQualifier();
        String q2 = v2.getQualifier();
        if (q1.startsWith("fuse-") && q2.startsWith("fuse-")) {
            q1 = cleanQualifierForComparison(q1);
            q2 = cleanQualifierForComparison(q2);
        }
        return q1.compareTo(q2);
    }

    private String cleanQualifierForComparison(String q) {
        return q.replace("-alpha-", "-").replace("-beta-", "-")
                .replace("-7-0-", "-70-")
                .replace("-7-", "-70-");
    }

    private Resource createResource(String uri, Attributes attributes) throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        for (Map.Entry attr : attributes.entrySet()) {
            headers.put(attr.getKey().toString(), attr.getValue().toString());
        }
        try {
            // TODO: provide correct logger
            return ResourceBuilder.build(null, uri, headers);
        } catch (BundleException e) {
            throw new Exception("Unable to create Resource for bundle " + uri, e);
        }
    }

    protected Attributes getAttributes(String uri, Map<String, File> urls, Map<String, FabBundleInfo> infos) throws Exception {
        InputStream is = DeploymentAgent.getBundleInputStream(uri, urls, infos);
        byte[] man = loadEntry(is, JarFile.MANIFEST_NAME);
        if (man == null)
        {
            throw new IllegalArgumentException("The specified url is not a valid jar (can't read manifest): " + uri);
        }
        Manifest manifest = new Manifest(new ByteArrayInputStream(man));
        return manifest.getMainAttributes();
    }

    private byte[] loadEntry(InputStream is, String name) throws IOException
    {
        try
        {
            ZipInputStream jis = new ZipInputStream(is);
            for (ZipEntry e = jis.getNextEntry(); e != null; e = jis.getNextEntry())
            {
                if (name.equalsIgnoreCase(e.getName()))
                {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int n;
                    while ((n = jis.read(buf, 0, buf.length)) > 0)
                    {
                        baos.write(buf, 0, n);
                    }
                    return baos.toByteArray();
                }
            }
        }
        finally
        {
            is.close();
        }
        return null;
    }

    /*
    TODO: re-enable requirements
    protected Resource parseRequirement(String req) throws Exception {
        int p = req.indexOf(':');
        String name;
        String filter;
        if (p > 0) {
            name = req.substring(0, p);
            filter = req.substring(p + 1);
        } else {
            if (req.contains("package")) {
                name = "package";
            } else if (req.contains("service")) {
                name = "service";
            } else {
                name = "bundle";
            }
            filter = req;
        }
        if (!filter.startsWith("(")) {
            filter = "(" + filter + ")";
        }
        ResourceImpl resource = new ResourceImpl();
        List<Requirement> requirements = ResourceBuilder.parseRequirement(null, resource, req);
        for (Requirement requirement : requirements) {
            resource.addRequirement(requirement);
        }
        return resource;
    }
    */

    private static class SimpleBundleInfo implements BundleInfo {
        private final String bundle;
        private final boolean dependency;

        public SimpleBundleInfo(String bundle, boolean dependency) {
            this.bundle = bundle;
            this.dependency = dependency;
        }

        @Override
        public String getLocation() {
            return bundle;
        }

        @Override
        public int getStartLevel() {
            return 0;
        }

        @Override
        public boolean isStart() {
            return true;
        }

        @Override
        public boolean isDependency() {
            return dependency;
        }
    }
}
