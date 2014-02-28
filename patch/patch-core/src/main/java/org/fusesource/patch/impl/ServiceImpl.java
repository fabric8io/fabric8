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
package org.fusesource.patch.impl;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.fusesource.patch.BundleUpdate;
import org.fusesource.patch.Patch;
import org.fusesource.patch.PatchException;
import org.fusesource.patch.Result;
import org.fusesource.patch.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.FrameworkWiring;

import static org.fusesource.patch.impl.Utils.close;
import static org.fusesource.patch.impl.Utils.copy;
import static org.fusesource.patch.impl.Utils.readFully;
import static org.fusesource.patch.impl.Utils.writeFully;

public class ServiceImpl implements Service {
    
    private final BundleContext bundleContext;
    private final File patchDir;

    private static final String ID = "id";
    private static final String DESCRIPTION = "description";
    private static final String DATE = "date";
    private static final String BUNDLES = "bundle";
    private static final String UPDATES = "update";
    private static final String COUNT = "count";
    private static final String RANGE = "range";
    private static final String SYMBOLIC_NAME = "symbolic-name";
    private static final String NEW_VERSION = "new-version";
    private static final String NEW_LOCATION = "new-location";
    private static final String OLD_VERSION = "old-version";
    private static final String OLD_LOCATION = "old-location";
    private static final String STARTUP = "startup";
    private static final String OVERRIDES = "overrides";

    private static final Pattern SYMBOLIC_NAME_PATTERN = Pattern.compile("([^;: ]+)(.*)");

    public ServiceImpl(BundleContext bundleContext) {
        // Use system bundle' bundle context to avoid running into
        // "Invalid BundleContext" exceptions when updating bundles
        this.bundleContext = bundleContext.getBundle(0).getBundleContext();
        String dir = this.bundleContext.getProperty("fuse.patch.location");
        patchDir = dir != null ? new File(dir) : this.bundleContext.getDataFile("patches");
        if (!patchDir.isDirectory()) {
            patchDir.mkdirs();
            if (!patchDir.isDirectory()) {
                throw new PatchException("Unable to create patch folder");
            }
        }
        load();
    }

    @Override
    public Iterable<Patch> getPatches() {
        return Collections.unmodifiableCollection(load().values());
    }

    @Override
    public Patch getPatch(String id) {
        return load().get(id);
    }

    @Override
    public Iterable<Patch> download(URL url) {
        try {
            File file = new File(patchDir, Long.toString(System.currentTimeMillis()) + ".patch.tmp");
            // Copy file
            InputStream is = null;
            OutputStream os = null;
            try {
                is = url.openStream();
                os = new FileOutputStream(file);
                copy(is, os);
            } finally {
                close( is, os );
            }
            // Patch file
            List<Patch> patches = new ArrayList<Patch>();
            // Try to unzip
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(file);
            } catch (IOException e) {
            }
            if (zipFile != null) {
                File localRepoPath = new File(System.getProperty("karaf.base"), "system");
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        String entryName = entry.getName();
                        if (entryName.startsWith("repository/")) {
                            String fileName = entryName.substring("repository/".length());
                            File f = new File(localRepoPath, fileName);
                            if (!f.isFile()) {
                                f.getParentFile().mkdirs();
                                InputStream fis = zipFile.getInputStream(entry);
                                FileOutputStream fos = new FileOutputStream(f);
                                try {
                                    copy(fis, fos);
                                } finally {
                                    close(fis, fos);
                                }
                            }
                        } else if (entryName.endsWith(".patch") && !entryName.contains("/")) {
                            File f = new File(patchDir, entryName);
                            if (!f.isFile()) {
                                InputStream fis = zipFile.getInputStream(entry);
                                FileOutputStream fos = new FileOutputStream(f);
                                try {
                                    copy(fis, fos);
                                } finally {
                                    close(fis, fos);
                                }
                            }
                            Patch patch = load(f);
                            f.renameTo(new File(patchDir, patch.getId() + ".patch"));
                            patches.add(patch);
                        }
                    }
                }
                close(zipFile);
                file.delete();
            }
            // If the file is not a zip/jar, assume it's a single patch file
            else {
                Patch patch = load(file);
                file.renameTo(new File(patchDir, patch.getId() + ".patch"));
                patches.add(patch);
            }
            return patches;
        } catch (Exception e) {
            throw new PatchException("Unable to download patch from url " + url, e);
        }
    }

    /**
     * Used by the patch client when executing the script in the console
     * @param ids
     */
    public void cliInstall(String[] ids) {
        final List<Patch> patches = new ArrayList<Patch>();
        for (String id : ids) {
            Patch patch = getPatch(id);
            if (patch == null) {
                throw new IllegalArgumentException("Unknown patch: " + id);
            }
            patches.add(patch);
        }
        install(patches, false, false);
    }

    Map<String, Patch> load() {
        Map<String, Patch> patches = new HashMap<String, Patch>();
        for (File file : patchDir.listFiles()) {
            if (file.exists() && file.getName().endsWith(".patch")) {
                try {
                    Patch patch = load(file);
                    patches.put(patch.getId(), patch);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return patches;
    }

    Patch load(File file) throws IOException {
        FileInputStream is = new FileInputStream(file);
        try {
            PatchImpl patch = doLoad(this, is);
            File fr = new File(file.getParent(), file.getName() + ".result");
            if (fr.isFile()) {
                patch.setResult(loadResult(patch, fr));
            }
            return patch;
        } finally {
            close(is);
        }
    }

    public static PatchImpl doLoad(ServiceImpl service, InputStream is) throws IOException {
        return new PatchImpl(service, PatchData.load(is));
    }

    Result loadResult(Patch patch, File file) throws IOException {
        Properties props = new Properties();
        FileInputStream is = new FileInputStream(file);
        try {
            props.load(is);
            long date = Long.parseLong(props.getProperty(DATE));
            List<BundleUpdate> updates = new ArrayList<BundleUpdate>();
            int count = Integer.parseInt(props.getProperty(UPDATES + "." + COUNT, "0"));
            for (int i = 0; i < count; i++) {
                String sn = props.getProperty(UPDATES + "." + Integer.toString(i) + "." + SYMBOLIC_NAME);
                String nv = props.getProperty(UPDATES + "." + Integer.toString(i) + "." + NEW_VERSION);
                String nl = props.getProperty(UPDATES + "." + Integer.toString(i) + "." + NEW_LOCATION);
                String ov = props.getProperty(UPDATES + "." + Integer.toString(i) + "." + OLD_VERSION);
                String ol = props.getProperty(UPDATES + "." + Integer.toString(i) + "." + OLD_LOCATION);
                updates.add(new BundleUpdateImpl(sn, nv, nl, ov, ol));
            }
            String startup = props.getProperty(STARTUP);
            String overrides = props.getProperty(OVERRIDES);
            return new ResultImpl(patch, false, date, updates, startup, overrides);
        } finally {
            close(is);
        }
    }

    void saveResult(Result result) throws IOException {
        File file = new File(patchDir, result.getPatch().getId() + ".patch.result");
        Properties props  = new Properties();
        FileOutputStream fos = new FileOutputStream(file);
        try {
            props.put(DATE, Long.toString(result.getDate()));
            props.put(UPDATES + "." + COUNT, Integer.toString(result.getUpdates().size()));
            int i = 0;
            for (BundleUpdate update : result.getUpdates()) {
                props.put(UPDATES + "." + Integer.toString(i) + "." + SYMBOLIC_NAME, update.getSymbolicName());
                props.put(UPDATES + "." + Integer.toString(i) + "." + NEW_VERSION, update.getNewVersion());
                props.put(UPDATES + "." + Integer.toString(i) + "." + NEW_LOCATION, update.getNewLocation());
                props.put(UPDATES + "." + Integer.toString(i) + "." + OLD_VERSION, update.getPreviousVersion());
                props.put(UPDATES + "." + Integer.toString(i) + "." + OLD_LOCATION, update.getPreviousLocation());
                i++;
            }
            props.put(STARTUP, ((ResultImpl) result).getStartup());
            String overrides = ((ResultImpl) result).getOverrides();
            if (overrides != null) {
                props.put(OVERRIDES, overrides);
            }
            props.store(fos, "Installation results for patch " + result.getPatch().getId());
        } finally {
            close(fos);
        }
    }

    void rollback(Patch patch, boolean force) throws PatchException {
        Result result = patch.getResult();
        if (result == null) {
            throw new PatchException("Patch " + patch.getId() + " is not installed");
        }
        Bundle[] allBundles = bundleContext.getBundles();
        List<BundleUpdate> badUpdates = new ArrayList<BundleUpdate>();
        for (BundleUpdate update : result.getUpdates()) {
            boolean found = false;
            Version v = Version.parseVersion(update.getNewVersion());
            for (Bundle bundle : allBundles) {
                if (stripSymbolicName(bundle.getSymbolicName()).equals(stripSymbolicName(update.getSymbolicName()))
                        && bundle.getVersion().equals(v)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                badUpdates.add(update);
            }
        }
        if (!badUpdates.isEmpty() && !force) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unable to rollback patch ").append(patch.getId()).append(" because of the following missing bundles:\n");
            for (BundleUpdate up : badUpdates) {
                sb.append("\t").append(up.getSymbolicName()).append("/").append(up.getNewVersion()).append("\n");
            }
            throw new PatchException(sb.toString());
        }

        Map<Bundle, String> toUpdate = new HashMap<Bundle, String>();
        for (BundleUpdate update : result.getUpdates()) {
            Version v = Version.parseVersion(update.getNewVersion());
            for (Bundle bundle : allBundles) {
                if (stripSymbolicName(bundle.getSymbolicName()).equals(stripSymbolicName(update.getSymbolicName()))
                        && bundle.getVersion().equals(v)) {
                    toUpdate.put(bundle, update.getPreviousLocation());
                }
            }
        }
        try {
            applyChanges(toUpdate);
            writeFully(new File(System.getProperty("karaf.base"), "etc/startup.properties"), ((ResultImpl) result).getStartup());
            writeFully(new File(System.getProperty("karaf.base"), "etc/overrides.properties"), ((ResultImpl) result).getOverrides());
        } catch (Exception e) {
            throw new PatchException("Unable to rollback patch " + patch.getId() + ": " + e.getMessage(), e);
        }
        ((PatchImpl) patch).setResult(null);
        File file = new File(patchDir, result.getPatch().getId() + ".patch.result");
        file.delete();
    }

    Result install(Patch patch, boolean simulate) {
        return install(patch, simulate, true);
    }

    Result install(Patch patch, boolean simulate, boolean synchronous) {
        Map<String, Result> results = install(Collections.singleton(patch), simulate, synchronous);
        return results.get(patch.getId());
    }

    Map<String, Result> install(final Collection<Patch> patches, boolean simulate, boolean synchronous) {
        try {
            // Compute individual patch results
            final Map<String, Result> results = new LinkedHashMap<String, Result>();
            final Map<Bundle, String> toUpdate = new HashMap<Bundle, String>();
            Map<String, BundleUpdate> allUpdates = new HashMap<String, BundleUpdate>();
            for (Patch patch : patches) {
                String startup = readFully(new File(System.getProperty("karaf.base"), "etc/startup.properties"));
                String overrides = readFully(new File(System.getProperty("karaf.base"), "etc/overrides.properties"));
                List<BundleUpdate> updates = new ArrayList<BundleUpdate>();
                Bundle[] allBundles = bundleContext.getBundles();
                for (String url : patch.getBundles()) {
                    JarInputStream jis = new JarInputStream(new URL(url).openStream());
                    jis.close();
                    Manifest manifest = jis.getManifest();
                    Attributes att = manifest != null ? manifest.getMainAttributes() : null;
                    String sn = att != null ? att.getValue(Constants.BUNDLE_SYMBOLICNAME) : null;
                    String vr = att != null ? att.getValue(Constants.BUNDLE_VERSION) : null;
                    if (sn == null || vr == null) {
                        continue;
                    }
                    Version v = VersionTable.getVersion(vr);

                    VersionRange range = null;

                    if (patch.getVersionRange(url) == null) {
                        // default version range starts with x.y.0 as the lower bound
                        Version lower = new Version(v.getMajor(), v.getMinor(), 0);

                        // We can't really upgrade with versions such as 2.1.0
                        if (v.compareTo(lower) > 0) {
                            range = new VersionRange(false, lower, v, true);
                        }
                    } else {
                        range = new VersionRange(patch.getVersionRange(url));
                    }

                    if (range != null) {
                        for (Bundle bundle : allBundles) {
                            Version oldV = bundle.getVersion();
                            if (bundle.getBundleId() != 0 && stripSymbolicName(sn).equals(stripSymbolicName(bundle.getSymbolicName())) && range.contains(oldV)) {
                                String location = bundle.getLocation();
                                BundleUpdate update = new BundleUpdateImpl(sn, v.toString(), url, oldV.toString(), location);
                                updates.add(update);
                                // Merge result
                                BundleUpdate oldUpdate = allUpdates.get(sn);
                                if (oldUpdate != null) {
                                    Version upv = VersionTable.getVersion(oldUpdate.getNewVersion());
                                    if (upv.compareTo(v) < 0) {
                                        allUpdates.put(sn, update);
                                        toUpdate.put(bundle, url);
                                    }
                                } else {
                                    toUpdate.put(bundle, url);
                                }
                            }
                        }
                    } else {
                        System.err.printf("Skipping bundle %s - unable to process bundle without a version range configuration%n", url);
                    }
                }
                if (!simulate) {
                    new Offline(new File(System.getProperty("karaf.base")))
                            .applyConfigChanges(((PatchImpl) patch).getPatch());
                }
                Result result = new ResultImpl(patch, simulate, System.currentTimeMillis(), updates, startup, overrides);
                results.put(patch.getId(), result);
            }
            // Apply results
            System.out.println("Bundles to update:");
            for (Map.Entry<Bundle, String> e : toUpdate.entrySet()) {
                System.out.println("    " + e.getKey().getSymbolicName() + "/" + e.getKey().getVersion().toString() + " with " + e.getValue());
            }
            if (simulate) {
                System.out.println("Running simulation only - no bundles are being updated at this time");
            } else {
                System.out.println("Installation will begin.  The connection may be lost or the console restarted.");
            }
            System.out.flush();
            if (!simulate) {
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            applyChanges(toUpdate);
                            for (Patch patch : patches) {
                                Result result = results.get(patch.getId());
                                ((PatchImpl) patch).setResult(result);
                                saveResult(result);
                            }
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                            System.err.flush();
                        }
                    }
                };
                if (synchronous) {
                    thread.run();
                } else {
                    thread.start();
                }
            }
            return results;
        } catch (Exception e) {
            throw new PatchException(e);
        }
    }

    private void applyChanges(Map<Bundle, String> toUpdate) throws BundleException, IOException {
        List<Bundle> toStop = new ArrayList<Bundle>();
        toStop.addAll(toUpdate.keySet());
        while (!toStop.isEmpty()) {
            List<Bundle> bs = getBundlesToDestroy(toStop);
            for (Bundle bundle : bs) {
                String hostHeader = (String) bundle.getHeaders().get(Constants.FRAGMENT_HOST);
                if (hostHeader == null && (bundle.getState() == Bundle.ACTIVE || bundle.getState() == Bundle.STARTING)) {
                    bundle.stop();
                }
                toStop.remove(bundle);
            }
        }
        Set<Bundle> toRefresh = new HashSet<Bundle>();
        Set<Bundle> toStart = new HashSet<Bundle>();
        for (Map.Entry<Bundle, String> e : toUpdate.entrySet()) {
            InputStream is = new URL(e.getValue()).openStream();
            try {
                Bundle bundle = e.getKey();
                bundle.update(is);
                toRefresh.add(bundle);
                toStart.add(bundle);
            } finally {
                is.close();
            }
        }
        findBundlesWithOptionalPackagesToRefresh(toRefresh);
        findBundlesWithFramentsToRefresh(toRefresh);
        if (!toRefresh.isEmpty()) {
            final CountDownLatch l = new CountDownLatch(1);
            FrameworkListener listener = new FrameworkListener() {
                @Override
                public void frameworkEvent(FrameworkEvent event) {
                    l.countDown();
                }
            };
            FrameworkWiring wiring = (FrameworkWiring) bundleContext.getBundle(0).adapt(FrameworkWiring.class);
            wiring.refreshBundles((Collection<Bundle>) toRefresh, new FrameworkListener[]{listener});
            try {
                l.await();
            } catch (InterruptedException e) {
                throw new PatchException("Bundle refresh interrupted", e);
            }
        }
        for (Bundle bundle : toStart) {
            String hostHeader = (String) bundle.getHeaders().get(Constants.FRAGMENT_HOST);
            if (hostHeader == null) {
                bundle.start();
            }
        }
    }

    private List<Bundle> getBundlesToDestroy(List<Bundle> bundles) {
        List<Bundle> bundlesToDestroy = new ArrayList<Bundle>();
        for (Bundle bundle : bundles) {
            ServiceReference[] references = bundle.getRegisteredServices();
            int usage = 0;
            if (references != null) {
                for (ServiceReference reference : references) {
                    usage += getServiceUsage(reference, bundles);
                }
            }
            if (usage == 0) {
                bundlesToDestroy.add(bundle);
            }
        }
        if (!bundlesToDestroy.isEmpty()) {
            Collections.sort(bundlesToDestroy, new Comparator<Bundle>() {
                public int compare(Bundle b1, Bundle b2) {
                    return (int) (b2.getLastModified() - b1.getLastModified());
                }
            });
        } else {
            ServiceReference ref = null;
            for (Bundle bundle : bundles) {
                ServiceReference[] references = bundle.getRegisteredServices();
                for (ServiceReference reference : references) {
                    if (getServiceUsage(reference, bundles) == 0) {
                        continue;
                    }
                    if (ref == null || reference.compareTo(ref) < 0) {
                        ref = reference;
                    }
                }
            }
            if (ref != null) {
                bundlesToDestroy.add(ref.getBundle());
            }
        }
        return bundlesToDestroy;
    }

    private static int getServiceUsage(ServiceReference ref, List<Bundle> bundles) {
        Bundle[] usingBundles = ref.getUsingBundles();
        int nb = 0;
        if (usingBundles != null) {
            for (Bundle bundle : usingBundles) {
                if (bundles.contains(bundle)) {
                    nb++;
                }
            }
        }
        return nb;
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

    /**
     * Strips symbolic name from directives.
     * @param symbolicName
     * @return
     */
    static String stripSymbolicName(String symbolicName) {
        Matcher m = SYMBOLIC_NAME_PATTERN.matcher(symbolicName);
        if (m.matches() && m.groupCount() >= 1) {
            return m.group(1);
        } else {
            return symbolicName;
        }
    }
}
