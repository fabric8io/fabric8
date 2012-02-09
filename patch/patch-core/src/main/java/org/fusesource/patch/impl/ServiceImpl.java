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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
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
import org.osgi.framework.Version;
import org.osgi.framework.wiring.FrameworkWiring;

public class ServiceImpl implements Service {
    
    private final BundleContext bundleContext;
    private final File patchDir;
    private final Map<String, Patch> patches = new HashMap<String, Patch>();

    private static final String ID = "id";
    private static final String DESCRIPTION = "description";
    private static final String DATE = "date";
    private static final String BUNDLES = "bundle";
    private static final String UPDATES = "update";
    private static final String COUNT = "count";
    private static final String SYMBOLIC_NAME = "symbolic-name";
    private static final String NEW_VERSION = "new-version";
    private static final String OLD_VERSION = "old-version";
    private static final String OLD_LOCATION = "old-location";

    public ServiceImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        String dir = bundleContext.getProperty("fuse.patch.location");
        patchDir = dir != null ? new File(dir) : this.bundleContext.getDataFile("patches");
        if (!patchDir.isDirectory()) {
            patchDir.mkdirs();
            if (!patchDir.isDirectory()) {
                throw new PatchException("Unable to create patch folder");
            }
        }
        reload();
    }

    @Override
    public Iterable<Patch> getPatches() {
        return Collections.unmodifiableCollection(patches.values());
    }

    @Override
    public Patch getPatch(String id) {
        return patches.get(id);
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
            // Add them to the list of downloaded patches
            for (Patch patch : patches) {
                this.patches.put(patch.getId(), patch);
            }
            return patches;
        } catch (Exception e) {
            throw new PatchException("Unable to download patch from url " + url, e);
        }
    }

    void reload() {
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
    }

    Patch load(File file) throws IOException {
        Properties props = new Properties();
        FileInputStream is = new FileInputStream(file);
        try {
            props.load(is);
            String id = props.getProperty(ID);
            String desc = props.getProperty(DESCRIPTION);
            List<String> bundles = new ArrayList<String>();
            int count = Integer.parseInt(props.getProperty(BUNDLES + "." + COUNT, "0"));
            for (int i = 0; i < count; i++) {
                bundles.add(props.getProperty(BUNDLES + "." + Integer.toString(i)));
            }
            PatchImpl patch = new PatchImpl(this, id, desc, bundles);
            File fr = new File(file.getParent(), file.getName() + ".result");
            if (fr.isFile()) {
                patch.setResult(loadResult(patch, fr));
            }
            return patch;
        } finally {
            close(is);
        }
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
                String ov = props.getProperty(UPDATES + "." + Integer.toString(i) + "." + OLD_VERSION);
                String ol = props.getProperty(UPDATES + "." + Integer.toString(i) + "." + OLD_LOCATION);
                updates.add(new BundleUpdateImpl(sn, nv, ov, ol));
            }
            return new ResultImpl(patch, false, date, updates);
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
                props.put(UPDATES + "." + Integer.toString(i) + "." + OLD_VERSION, update.getPreviousVersion());
                props.put(UPDATES + "." + Integer.toString(i) + "." + OLD_LOCATION, update.getPreviousLocation());
            }
            props.store(fos, "Installation results for patch " + result.getPatch().getId());
        } finally {
            close(fos);
        }
    }

    void rollback(PatchImpl patch, boolean force) throws PatchException {
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
                if (bundle.getSymbolicName().equals(update.getSymbolicName())
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

        Set<Bundle> toRefresh = new HashSet<Bundle>();
        Set<Bundle> toDelete = new HashSet<Bundle>();
        Set<String> toInstall = new HashSet<String>();
        for (BundleUpdate update : result.getUpdates()) {
            Version v = Version.parseVersion(update.getNewVersion());
            for (Bundle bundle : allBundles) {
                if (bundle.getSymbolicName().equals(update.getSymbolicName())
                        && bundle.getVersion().equals(v)) {
                    toInstall.add(update.getPreviousLocation());
                    toDelete.add(bundle);
                }
            }
        }
        try {
            applyChanges(toRefresh, toDelete, toInstall);
        } catch (Exception e) {
            throw new PatchException("Unable to rollback patch " + patch.getId() + ": " + e.getMessage(), e);
        }
        patch.setResult(null);
        File file = new File(patchDir, result.getPatch().getId() + ".patch.result");
        file.delete();
    }

    Result install(PatchImpl patch, boolean simulate) {
        try {
            Set<Bundle> toRefresh = new HashSet<Bundle>();
            Set<Bundle> toDelete = new HashSet<Bundle>();
            Set<String> toInstall = new HashSet<String>();
            List<BundleUpdate> updates = new ArrayList<BundleUpdate>();
            Bundle[] allBundles = bundleContext.getBundles();
            for (String url : patch.getBundles()) {
                JarInputStream jis = new JarInputStream(new URL(url).openStream());
                Attributes att = jis.getManifest().getMainAttributes();
                jis.close();
                String sn = att.getValue(Constants.BUNDLE_SYMBOLICNAME);
                String vr = att.getValue(Constants.BUNDLE_VERSION);
                Version v = VersionTable.getVersion(vr);
                // We can't really upgrade with versions such as 2.1.0
                if (v.getMicro() > 0) {
                    VersionRange range = new VersionRange(false, new Version(v.getMajor(), v.getMinor(), 0), v, true);
                    for (Bundle bundle : allBundles) {
                        Version oldV = bundle.getVersion();
                        if (sn.equals(bundle.getSymbolicName()) && range.contains(oldV)) {
                            String location = bundle.getLocation();
                            updates.add(new BundleUpdateImpl(sn, v.toString(), oldV.toString(), location));
                            toInstall.add(url);
                            toDelete.add(bundle);
                        }
                    }
                }
            }
    
            Result result = new ResultImpl(patch, simulate, System.currentTimeMillis(), updates);
            if (!simulate) {
                applyChanges(toRefresh, toDelete, toInstall);
                patch.setResult(result);
                saveResult(result);
            }
            return result;
        } catch (Exception e) {
            throw new PatchException(e);
        }
    }

    private void applyChanges(Set<Bundle> toRefresh, Set<Bundle> toDelete, Set<String> toInstall) throws BundleException {
        for (Bundle bundle : toDelete) {
            bundle.uninstall();
            toRefresh.add(bundle);
        }
        for (String url : toInstall) {
            Bundle bundle = bundleContext.installBundle(url);
            toRefresh.add(bundle);
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

    static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        try {
            byte[] buffer = new byte[8192];
            int len;
            for (; ;) {
                len = inputStream.read(buffer);
                if (len > 0) {
                    outputStream.write(buffer, 0, len);
                } else {
                    outputStream.flush();
                    break;
                }
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
            try {
                outputStream.close();
            } catch (IOException e) {
            }
        }
    }

    static void close(ZipFile... closeables) {
        for (ZipFile c : closeables) {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (IOException e) {
            }
        }
    }
    static void close(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (IOException e) {
            }
        }
    }

}
