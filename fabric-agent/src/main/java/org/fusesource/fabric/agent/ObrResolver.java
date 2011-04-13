/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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

    public List<Resource> resolve(Set<Feature> features) throws Exception {
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

}
