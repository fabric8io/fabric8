/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.agent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.properties.Properties;
import org.apache.felix.utils.version.VersionCleaner;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.FeatureValidationUtil;
import org.apache.karaf.features.internal.RepositoryImpl;
import org.fusesource.fabric.agent.download.DownloadManager;
import org.fusesource.fabric.agent.download.Future;
import org.fusesource.fabric.agent.download.FutureListener;
import org.fusesource.fabric.agent.mvn.MavenConfigurationImpl;
import org.fusesource.fabric.agent.mvn.MavenSettingsImpl;
import org.fusesource.fabric.agent.mvn.PropertiesPropertyResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeploymentAgent implements ManagedService, FrameworkListener {

    private static final String DEFAULT_VERSION = "0.0.0";
    private static final String FABRIC_ZOOKEEPER_PID = "fabric.zookeeper.id";

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentAgent.class);

    private BundleContext bundleContext;
    private PackageAdmin packageAdmin;
    private StartLevel startLevel;
    private ObrResolver obrResolver;

    private final Object refreshLock = new Object();
    private long refreshTimeout = 5000;

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private DownloadManager manager;

    public DeploymentAgent() throws MalformedURLException {
        final MavenConfigurationImpl config = new MavenConfigurationImpl(
                new PropertiesPropertyResolver(System.getProperties()), "org.ops4j.pax.url.mvn"
        );
        config.setSettings(new MavenSettingsImpl(config.getSettingsFileUrl(), config.useFallbackRepositories()));
        manager = new DownloadManager(config);
    }

    public StartLevel getStartLevel() {
        return startLevel;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public PackageAdmin getPackageAdmin() {
        return packageAdmin;
    }

    public ObrResolver getObrResolver() {
        return obrResolver;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setPackageAdmin(PackageAdmin packageAdmin) {
        this.packageAdmin = packageAdmin;
    }

    public void setStartLevel(StartLevel startLevel) {
        this.startLevel = startLevel;
    }

    public void setObrResolver(ObrResolver obrResolver) {
        this.obrResolver = obrResolver;
    }

    public void start() {
        loadState();
        bundleContext.addFrameworkListener(this);
    }

    public void stop() {
        bundleContext.removeFrameworkListener(this);
        manager.shutdown();
        executor.shutdown();
    }

    public void loadState() {

    }

    public void saveState() {

    }

    public void updated(final Dictionary props) throws ConfigurationException {
        executor.submit(new Runnable() {
            public void run() {
                try {
                    doUpdate(props);
                } catch (Exception e) {
                    LOGGER.error("Unable to update agent", e);
                }
            }
        });
    }

    public void doUpdate(Dictionary props) throws Exception {
        if (props == null) {
            return;
        }
        Map<String, String> properties = new HashMap<String, String>();
        for (Enumeration e = props.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            Object val = props.get(key);
            if (!"service.pid".equals(key) && !FABRIC_ZOOKEEPER_PID.equals(key)) {
                properties.put(key.toString(), val.toString());
            }
        }
        // Update framework if needed
        for (String key : properties.keySet()) {
            if (key.equals("framework")) {
                String url = properties.get(key);
                if (updateFramework(url)) {
                    return;
                }
            }
        }
        // Compute deployment
        Map<URI, Repository> repositories = new HashMap<URI, Repository>();
        for (String key : properties.keySet()) {
            if (key.startsWith("repository.")) {
                String url = properties.get(key);
                if (url == null || url.length() == 0) {
                    url = key.substring("repository.".length());
                }
                if (url != null && url.length() > 0) {
                    URI uri = URI.create(url);
                    addRepository(repositories, uri);
                }
            }
        }
        Set<Feature> features = new HashSet<Feature>();
        for (String key : properties.keySet()) {
            if (key.startsWith("feature.")) {
                String name = properties.get(key);
                if (name == null || name.length() == 0) {
                    name = key.substring("feature.".length());
                }
                Feature feature = search(name, repositories.values());
                if (feature == null) {
                    throw new IllegalArgumentException("Unable to find feature " + name);
                }
                features.add(feature);
            }
        }
        Set<String> bundles = new HashSet<String>();
        for (String key : properties.keySet()) {
            if (key.startsWith("bundle.")) {
                String url = properties.get(key);
                if (url == null || url.length() == 0) {
                    url = key.substring("bundle.".length());
                }
                if (url != null && url.length() > 0) {
                    bundles.add(url);
                }
            }
        }
        // Update bundles
        updateDeployment(repositories, features, bundles);
    }

    private void addRepository(Map<URI, Repository> repositories, URI uri) throws Exception {
        if (!repositories.containsKey(uri)) {
            FeatureValidationUtil.validate(uri);
            RepositoryImpl repo = new RepositoryImpl(uri);
            repositories.put(uri, repo);
            repo.load();
            for (URI ref : repo.getRepositories()) {
                addRepository(repositories, ref);
            }
        }
    }

    private Feature search(String key, Collection<Repository> repositories) {
        String[] split = key.split("/");
        String name = split[0].trim();
        String version = null;
        if (split.length == 2) {
            version = split[1].trim();
        }
        if (version == null || version.length() == 0) {
            version = DEFAULT_VERSION;
        }
        return search(name, version, repositories);
    }

    private Feature search(String name, String version, Collection<Repository> repositories) {
        VersionRange range = new VersionRange(version, false, true);
        Feature bestFeature = null;
        Version bestVersion = null;
        for (Repository repo : repositories) {
            Feature[] features;
            try {
                features = repo.getFeatures();
            } catch (Exception e) {
                // This should not happen as the repository has been loaded already
                throw new IllegalStateException(e);
            }
            for (Feature feature : features) {
                if (name.equals(feature.getName())) {
                    Version v = new Version(VersionCleaner.clean(feature.getVersion()));
                    if (range.contains(v)) {
                        if (bestVersion == null || bestVersion.compareTo(v) < 0) {
                            bestFeature = feature;
                            bestVersion = v;
                        }
                    }
                }
            }
        }
        return bestFeature;
    }

    private Set<Feature> addFeatures(Collection<Feature> features, Collection<Repository> repositories) {
        Set<Feature> set = new HashSet<Feature>();
        for (Feature feature : features) {
            addFeatures(set, feature, repositories);
        }
        return set;
    }

    private Set<Feature> addFeatures(Set<Feature> set, Feature feature, Collection<Repository> repositories) {
        set.add(feature);
        for (Feature dep : feature.getDependencies()) {
            Feature f = search(dep.getName(), dep.getVersion(), repositories);
            if (f == null) {
                throw new IllegalArgumentException("Unable to find feature " + dep.getName() + "/" + dep.getVersion());
            }
            addFeatures(set, f, repositories);
        }
        return set;
    }

    private void updateDeployment(Map<URI, Repository> repositories, Set<Feature> features, Set<String> bundles) throws Exception {
        Set<Feature> allFeatures = addFeatures(features, repositories.values());
        downloadBundles(allFeatures, bundles);
        List<Resource> toDeploy = getObrResolver().resolve(allFeatures, bundles);

        StringBuilder sb = new StringBuilder();
        sb.append("Configuration changed.  New bundles list:\n");
        for (Resource bundle : toDeploy) {
            sb.append("  ").append(bundle.getURI()).append("\n");
        }
        LOGGER.info(sb.toString());

        List<Resource> toInstall = new ArrayList<Resource>();
        List<Bundle> toDelete = new ArrayList<Bundle>();
        List<Bundle> toIgnore = new ArrayList<Bundle>();
        Map<Bundle, Resource> toUpdate = new HashMap<Bundle, Resource>();

        // First pass: go through all installed bundles and mark them
        // as either to ignore or delete
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getBundleId() != 0) {
                Resource resource = null;
                for (Resource res : toDeploy) {
                    if (res.getSymbolicName().equals(bundle.getSymbolicName())
                            && res.getVersion().equals(bundle.getVersion())) {
                        resource = res;
                        break;
                    }
                }
                if (resource != null) {
                    toIgnore.add(bundle);
                    toDeploy.remove(resource);
                } else {
                    toDelete.add(bundle);
                }
            }
        }

        // Second pass on remaining resources
        for (Resource resource : toDeploy) {
            TreeMap<Version, Bundle> matching = new TreeMap<Version, Bundle>();
            VersionRange range = getMicroVersionRange(resource.getVersion());
            for (Bundle bundle : toDelete) {
                if (bundle.getSymbolicName().equals(resource.getSymbolicName())
                        && range.contains(bundle.getVersion())) {
                    matching.put(bundle.getVersion(), bundle);
                }
            }
            if (!matching.isEmpty()) {
                Bundle bundle = matching.lastEntry().getValue();
                toUpdate.put(bundle, resource);
                toDelete.remove(bundle);
            } else {
                toInstall.add(resource);
            }
        }

        // Display
        LOGGER.info("Changes to perform:");
        LOGGER.info("  Bundles to uninstall:");
        for (Bundle bundle : toDelete) {
            LOGGER.info("    " + bundle.getSymbolicName() + " / " + bundle.getVersion());
        }
//        System.out.println("  Bundles to ignore:");
//        for (Bundle bundle : toIgnore) {
//            System.out.println("    " + bundle.getSymbolicName() + " / " + bundle.getVersion());
//        }
        LOGGER.info("  Bundles to update:");
        for (Map.Entry<Bundle, Resource> entry : toUpdate.entrySet()) {
            LOGGER.info("    " + entry.getKey().getSymbolicName() + " / " + entry.getKey().getVersion() + " with " + entry.getValue().getURI());
        }
        LOGGER.info("  Bundles to install:");
        for (Resource resource : toInstall) {
            LOGGER.info("    " + resource.getURI());
        }


        Set<Bundle> toRefresh = new HashSet<Bundle>();
        Set<Bundle> toStart = new HashSet<Bundle>();

        // Execute
        for (Bundle bundle : toDelete) {
            bundle.uninstall();
            toRefresh.add(bundle);
        }
        for (Map.Entry<Bundle, Resource> entry : toUpdate.entrySet()) {
            Bundle bundle = entry.getKey();
            Resource resource = entry.getValue();
            InputStream is = new URL(resource.getURI()).openStream();
            bundle.stop(Bundle.STOP_TRANSIENT);
            bundle.update(is);
            toRefresh.add(bundle);
            toStart.add(bundle);
        }
        for (Resource resource : toInstall) {
            Bundle bundle = bundleContext.installBundle(resource.getURI());
            toRefresh.add(bundle);
            toStart.add(bundle);
        }

        findBundlesWithOptionalPackagesToRefresh(toRefresh);
        findBundlesWithFramentsToRefresh(toRefresh);

        LOGGER.info("Refreshing bundles:");
        for (Bundle bundle : toRefresh) {
            LOGGER.info("  " + bundle.getSymbolicName() + " / " + bundle.getVersion());
        }

        if (!toRefresh.isEmpty()) {
            refreshPackages(toRefresh.toArray(new Bundle[toRefresh.size()]));
        }

        LOGGER.info("Starting bundles:");
        for (Bundle bundle : toStart) {
            String hostHeader = (String) bundle.getHeaders().get(Constants.FRAGMENT_HOST);
            if (hostHeader == null) {
                bundle.start();
            }
        }

        LOGGER.info("Done.");
    }

    private VersionRange getMicroVersionRange(Version version) {
        Version floor = new Version(version.getMajor(), version.getMinor(), 0);
        Version ceil = new Version(version.getMajor(), version.getMinor() + 1, 0);
        return new VersionRange(false, floor, ceil, true);
    }


    protected void findBundlesWithFramentsToRefresh(Set<Bundle> toRefresh) {
        for (Bundle b : toRefresh) {
            if (b.getState() != Bundle.UNINSTALLED) {
                String hostHeader = (String) b.getHeaders().get(Constants.FRAGMENT_HOST);
                if (hostHeader != null) {
                    Clause[] clauses = Parser.parseHeader(hostHeader);
                    if (clauses != null && clauses.length > 0) {
                        Clause path = clauses[0];
                        for (Bundle hostBundle : bundleContext.getBundles()) {
                            if (hostBundle.getSymbolicName().equals(path.getName())) {
                                String ver = path.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
                                if (ver != null) {
                                    VersionRange v = VersionRange.parseVersionRange(ver);
                                    if (v.contains(hostBundle.getVersion())) {
                                        toRefresh.add(hostBundle);
                                    }
                                } else {
                                    toRefresh.add(hostBundle);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void findBundlesWithOptionalPackagesToRefresh(Set<Bundle> toRefresh) {
        // First pass: include all bundles contained in these features
        Set<Bundle> bundles = new HashSet<Bundle>(Arrays.asList(bundleContext.getBundles()));
        bundles.removeAll(toRefresh);
        if (bundles.isEmpty()) {
            return;
        }
        // Second pass: for each bundle, check if there is any unresolved optional package that could be resolved
        Map<Bundle, List<Clause>> imports = new HashMap<Bundle, List<Clause>>();
        for (Iterator<Bundle> it = bundles.iterator(); it.hasNext();) {
            Bundle b = it.next();
            String importsStr = (String) b.getHeaders().get(Constants.IMPORT_PACKAGE);
            List<Clause> importsList = getOptionalImports(importsStr);
            if (importsList.isEmpty()) {
                it.remove();
            } else {
                imports.put(b, importsList);
            }
        }
        if (bundles.isEmpty()) {
            return;
        }
        // Third pass: compute a list of packages that are exported by our bundles and see if
        //             some exported packages can be wired to the optional imports
        List<Clause> exports = new ArrayList<Clause>();
        for (Bundle b : toRefresh) {
            if (b.getState() != Bundle.UNINSTALLED) {
                String exportsStr = (String) b.getHeaders().get(Constants.EXPORT_PACKAGE);
                if (exportsStr != null) {
                    Clause[] exportsList = Parser.parseHeader(exportsStr);
                    exports.addAll(Arrays.asList(exportsList));
                }
            }
        }
        for (Iterator<Bundle> it = bundles.iterator(); it.hasNext();) {
            Bundle b = it.next();
            List<Clause> importsList = imports.get(b);
            for (Iterator<Clause> itpi = importsList.iterator(); itpi.hasNext();) {
                Clause pi = itpi.next();
                boolean matching = false;
                for (Clause pe : exports) {
                    if (pi.getName().equals(pe.getName())) {
                        String evStr = pe.getAttribute(Constants.VERSION_ATTRIBUTE);
                        String ivStr = pi.getAttribute(Constants.VERSION_ATTRIBUTE);
                        Version exported = evStr != null ? Version.parseVersion(evStr) : Version.emptyVersion;
                        VersionRange imported = ivStr != null ? VersionRange.parseVersionRange(ivStr) : VersionRange.ANY_VERSION;
                        if (imported.contains(exported)) {
                            matching = true;
                            break;
                        }
                    }
                }
                if (!matching) {
                    itpi.remove();
                }
            }
            if (importsList.isEmpty()) {
                it.remove();
//            } else {
//                LOGGER.debug("Refeshing bundle {} ({}) to solve the following optional imports", b.getSymbolicName(), b.getBundleId());
//                for (Clause p : importsList) {
//                    LOGGER.debug("    {}", p);
//                }
//
            }
        }
        toRefresh.addAll(bundles);
    }

    protected List<Clause> getOptionalImports(String importsStr) {
        Clause[] imports = Parser.parseHeader(importsStr);
        List<Clause> result = new LinkedList<Clause>();
        for (int i = 0; i < imports.length; i++) {
            String resolution = imports[i].getDirective(Constants.RESOLUTION_DIRECTIVE);
            if (Constants.RESOLUTION_OPTIONAL.equals(resolution)) {
                result.add(imports[i]);
            }
        }
        return result;
    }

    protected boolean updateFramework(String url) throws Exception {
        if (!url.startsWith("mvn:")) {
            throw new IllegalArgumentException("Framework url must use the mvn: protocol");
        }
        File file = manager.download(url).await().getFile();
        String path = file.getPath();
        if (path.startsWith(System.getProperty("karaf.home"))) {
            path = path.substring(System.getProperty("karaf.home").length() + 1);
        }
        Properties properties = new Properties(new File(System.getProperty("karaf.base") + File.separator + "etc" + File.separator + "config.properties"));
        if (!path.equals(properties.get("karaf.framework.felix"))) {
            properties.put("karaf.framework", "felix");
            properties.put("karaf.framework.felix", path);
            properties.save();
            System.setProperty("karaf.restart", "true");
            bundleContext.getBundle(0).stop();
            return true;
        }
        return false;
    }

    protected void downloadBundles(Set<Feature> features, Set<String> bundles) throws IOException, InterruptedException {
        Set<String> locations = new HashSet<String>();
        for (Feature feature : features) {
            for (BundleInfo bundle : feature.getBundles()) {
                locations.add(bundle.getLocation());
            }
        }
        for (String bundle : bundles) {
            locations.add(bundle);
        }
        final CountDownLatch latch = new CountDownLatch(locations.size());
        final FutureListener listener = new FutureListener() {
            public void operationComplete(Future future) {
                latch.countDown();
            }
        };
        for (String location : locations) {
            manager.download(location).addListener(listener);
        }
        latch.await();
    }

    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
            synchronized (refreshLock) {
                refreshLock.notifyAll();
            }
        }
    }

    protected void refreshPackages(Bundle[] bundles) throws InterruptedException {
        if (getPackageAdmin() != null) {
            synchronized (refreshLock) {
                getPackageAdmin().refreshPackages(bundles);
                refreshLock.wait(refreshTimeout);
            }
        }
    }

}
