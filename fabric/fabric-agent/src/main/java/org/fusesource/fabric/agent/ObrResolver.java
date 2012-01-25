/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.agent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.osgi.framework.InvalidSyntaxException;

public class ObrResolver {

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

    public List<Resource> resolve(Set<Feature> features, Set<String> bundles) throws Exception {
        List<Requirement> reqs = new ArrayList<Requirement>();
        List<Resource> ress = new ArrayList<Resource>();
        List<Resource> deploy = new ArrayList<Resource>();
        Map<Object, BundleInfo> infos = new HashMap<Object, BundleInfo>();
        for (Feature feature : features) {
            for (BundleInfo bundleInfo : feature.getBundles()) {
                try {
                    URL url = new URL(bundleInfo.getLocation());
                    Resource res = repositoryAdmin.getHelper().createResource(url);
                    ress.add(res);
                    infos.put(res, bundleInfo);
                } catch (MalformedURLException e) {
                    Requirement req = parseRequirement(bundleInfo.getLocation());
                    reqs.add(req);
                    infos.put(req, bundleInfo);
                }
            }
            for (final String bundle : bundles) {
                URL url = new URL(bundle);
                Resource res = repositoryAdmin.getHelper().createResource(url);
                ress.add(res);
                infos.put(res, new SimpleBundleInfo(bundle));
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

        public SimpleBundleInfo(String bundle) {
            this.bundle = bundle;
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
            return false;
        }
    }
}
