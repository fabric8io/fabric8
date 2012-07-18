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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.ResourceImpl;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.osgi.FabBundleInfo;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObrResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObrResolver.class);

    private RepositoryAdmin repositoryAdmin;

    public ObrResolver() {
    }

    public ObrResolver(RepositoryAdmin repositoryAdmin) {
        this.repositoryAdmin = repositoryAdmin;
    }

    public RepositoryAdmin getRepositoryAdmin() {
        return repositoryAdmin;
    }

    public void setRepositoryAdmin(RepositoryAdmin repositoryAdmin) {
        this.repositoryAdmin = repositoryAdmin;
    }

    public List<Resource> resolve(Set<Feature> features, Set<String> bundles,
                                  Map<String, FabBundleInfo> fabs,
                                  Map<String, File> downloads) throws Exception {
        List<Requirement> reqs = new ArrayList<Requirement>();
        List<Resource> ress = new ArrayList<Resource>();
        List<Resource> deploy = new ArrayList<Resource>();
        Map<Object, BundleInfo> infos = new HashMap<Object, BundleInfo>();
        for (Feature feature : features) {
            for (BundleInfo bundleInfo : feature.getBundles()) {
                try {
                    //We ignore Fabs completely as the are already been added to fabs set.
                    if (!bundleInfo.getLocation().startsWith(DeploymentAgent.FAB_PROTOCOL)) {
                        Resource res = createResource(bundleInfo.getLocation(), downloads, fabs);
                        if (res == null) {
                            throw new IllegalArgumentException("Unable to build OBR representation for bundle " + bundleInfo.getLocation());
                        }
                        ress.add(res);
                        infos.put(res, bundleInfo);
                    }
                } catch (MalformedURLException e) {
                    Requirement req = parseRequirement(bundleInfo.getLocation());
                    reqs.add(req);
                    infos.put(req, bundleInfo);
                }
            }
        }
        for (String bundle : bundles) {
            Resource res = createResource(bundle, downloads, fabs);
            if (res == null) {
                throw new IllegalArgumentException("Unable to build OBR representation for bundle " + bundle);
            }
            ress.add(res);
            infos.put(res, new SimpleBundleInfo(bundle, false));
        }
        for (FabBundleInfo fab : fabs.values()) {
            Resource res = repositoryAdmin.getHelper().createResource(fab.getManifest());
            if (res == null) {
                throw new IllegalArgumentException("Unable to build OBR representation for fab " + fab.getUrl());
            }
            ((ResourceImpl) res).put(Resource.URI, DeploymentAgent.FAB_PROTOCOL + fab.getUrl(), Property.URI);
            ress.add(res);
            infos.put(res, new SimpleBundleInfo(fab.getUrl(), false));
            for (DependencyTree dep : fab.getBundles()) {
                if (dep.isBundle()) {
                    URL url = new URL(dep.getUrl());
                    Resource resDep = createResource(dep.getUrl(), downloads, fabs);
                    if (resDep == null) {
                        throw new IllegalArgumentException("Unable to build OBR representation for fab dependency " + url);
                    }
                    ress.add(resDep);
                    infos.put(resDep, new SimpleBundleInfo(dep.getUrl(), true));
                }
            }
        }

        Repository repository = repositoryAdmin.getHelper().repository(ress.toArray(new Resource[ress.size()]));
        List<Repository> repos = new ArrayList<Repository>();
        repos.add(repositoryAdmin.getSystemRepository());
        repos.add(repository);
        repos.addAll(Arrays.asList(repositoryAdmin.listRepositories()));
        org.apache.felix.bundlerepository.Resolver resolver = repositoryAdmin.resolver(repos.toArray(new Repository[repos.size()]));

        for (Resource res : ress) {
            if (!infos.get(res).isDependency()) {
                resolver.add(res);
            }
        }
        for (Requirement req : reqs) {
            resolver.add(req);
        }

        if (!resolver.resolve(org.apache.felix.bundlerepository.Resolver.NO_OPTIONAL_RESOURCES)) {
            StringWriter w = new StringWriter();
            PrintWriter out = new PrintWriter(w);
            Reason[] failedReqs = resolver.getUnsatisfiedRequirements();
            if ((failedReqs != null) && (failedReqs.length > 0)) {
                out.println("Unsatisfied requirement(s):");
                printUnderline(out, 27);
                for (Reason r : failedReqs) {
                    out.println("   " + r.getRequirement().getName() + ":" + r.getRequirement().getFilter());
                    out.println("      " + r.getResource().getPresentationName());
                }
            } else {
                out.println("Could not resolve targets.");
            }
            out.flush();
            throw new Exception("Can not resolve feature:\n" + w.toString());
        }

        Collections.addAll(deploy, resolver.getAddedResources());
        Collections.addAll(deploy, resolver.getRequiredResources());
        return deploy;
    }

    protected Resource createResource(String uri, Map<String, File> urls, Map<String, FabBundleInfo> infos) throws Exception {
        URL url = new URL(uri);
        Attributes attributes = getAttributes(uri, urls, infos);
        ResourceImpl resource = (ResourceImpl) repositoryAdmin.getHelper().createResource(attributes);
        if (resource != null) {
            if ("file".equals(url.getProtocol()))
            {
                try {
                    File f = new File(url.toURI());
                    resource.put(Resource.SIZE, Long.toString(f.length()), null);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            resource.put(Resource.URI, url.toExternalForm(), null);
        }
        return resource;
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

    protected void printUnderline(PrintWriter out, int length) {
        for (int i = 0; i < length; i++) {
            out.print('-');
        }
        out.println("");
    }

    protected Requirement parseRequirement(String req) throws InvalidSyntaxException {
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
        return repositoryAdmin.getHelper().requirement(name, filter);
    }

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
