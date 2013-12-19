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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import junit.framework.Assert;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.fusesource.patch.Patch;
import org.fusesource.patch.Result;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.FrameworkWiring;

import static org.easymock.EasyMock.*;
import static org.fusesource.patch.impl.Utils.copy;
import static org.fusesource.patch.impl.Utils.readFully;
import static org.fusesource.patch.impl.Utils.writeFully;
import static org.junit.Assert.*;
import static org.fusesource.patch.impl.ServiceImpl.stripSymbolicName;

public class ServiceImplTest {

    File baseDir;

    File karaf;
    File storage;
    File bundlev131;
    File bundlev132;
    File bundlev140;
    File bundlev200;
    File patch132;
    File patch140;
    File patch200;

    @Before
    public void setUp() throws Exception {
        URL base = getClass().getClassLoader().getResource("log4j.properties");
        try {
            baseDir = new File(base.toURI()).getParentFile();
        } catch(URISyntaxException e) {
            baseDir = new File(base.getPath()).getParentFile();
        }

        URL.setURLStreamHandlerFactory(new CustomBundleURLStreamHandlerFactory());
        generateData();
    }

    @After
    public void tearDown() throws Exception {
        Field field = URL.class.getDeclaredField("factory");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    public void testOfflineOverrides() throws IOException {
        Offline offline = new Offline(karaf);
        String startup;
        String overrides;

        offline.apply(patch132);
        startup = readFully(new File(karaf, "etc/startup.properties"));
        overrides = readFully(new File(karaf, "etc/overrides.properties"));

        assertEquals("", startup.trim());
        assertEquals("mvn:foo/my-bsn/1.3.2", overrides.trim());
        assertTrue(new File(karaf, "system/foo/my-bsn/1.3.2/my-bsn-1.3.2.jar").exists());
        assertFalse(new File(karaf, "system/foo/my-bsn/1.4.0/my-bsn-1.4.0.jar").exists());
        assertFalse(new File(karaf, "system/foo/my-bsn/2.0.0/my-bsn-2.0.0.jar").exists());

        offline.apply(patch140);
        startup = readFully(new File(karaf, "etc/startup.properties"));
        overrides = readFully(new File(karaf, "etc/overrides.properties"));

        assertEquals("", startup.trim());
        assertEquals("mvn:foo/my-bsn/1.4.0;range=[1.3.0,1.5.0)", overrides.trim());
        assertFalse(new File(karaf, "system/foo/my-bsn/1.3.2/my-bsn-1.3.2.jar").exists());
        assertTrue(new File(karaf, "system/foo/my-bsn/1.4.0/my-bsn-1.4.0.jar").exists());
        assertFalse(new File(karaf, "system/foo/my-bsn/2.0.0/my-bsn-2.0.0.jar").exists());

        offline.apply(patch200);
        startup = readFully(new File(karaf, "etc/startup.properties"));
        overrides = readFully(new File(karaf, "etc/overrides.properties"));

        assertEquals("", startup.trim());
        assertEquals("mvn:foo/my-bsn/1.4.0;range=[1.3.0,1.5.0)\nmvn:foo/my-bsn/2.0.0", overrides.trim());
        assertFalse(new File(karaf, "system/foo/my-bsn/1.3.2/my-bsn-1.3.2.jar").exists());
        assertTrue(new File(karaf, "system/foo/my-bsn/1.4.0/my-bsn-1.4.0.jar").exists());
        assertTrue(new File(karaf, "system/foo/my-bsn/2.0.0/my-bsn-2.0.0.jar").exists());
    }

    @Test
    public void testOfflineStartup() throws IOException {
        Offline offline = new Offline(karaf);
        String startup;
        String overrides;

        writeFully(new File(karaf, "etc/startup.properties"), "foo/my-bsn/1.3.1/my-bsn-1.3.1.jar=1");

        offline.apply(patch132);
        startup = readFully(new File(karaf, "etc/startup.properties"));
        overrides = readFully(new File(karaf, "etc/overrides.properties"));

        assertEquals("foo/my-bsn/1.3.2/my-bsn-1.3.2.jar=1", startup.trim());
        assertEquals("mvn:foo/my-bsn/1.3.2", overrides.trim());
        assertTrue(new File(karaf, "system/foo/my-bsn/1.3.2/my-bsn-1.3.2.jar").exists());
        assertFalse(new File(karaf, "system/foo/my-bsn/1.4.0/my-bsn-1.4.0.jar").exists());
        assertFalse(new File(karaf, "system/foo/my-bsn/2.0.0/my-bsn-2.0.0.jar").exists());

        offline.apply(patch140);
        startup = readFully(new File(karaf, "etc/startup.properties"));
        overrides = readFully(new File(karaf, "etc/overrides.properties"));

        assertEquals("foo/my-bsn/1.4.0/my-bsn-1.4.0.jar=1", startup.trim());
        assertEquals("mvn:foo/my-bsn/1.4.0;range=[1.3.0,1.5.0)", overrides.trim());
        assertFalse(new File(karaf, "system/foo/my-bsn/1.3.2/my-bsn-1.3.2.jar").exists());
        assertTrue(new File(karaf, "system/foo/my-bsn/1.4.0/my-bsn-1.4.0.jar").exists());
        assertFalse(new File(karaf, "system/foo/my-bsn/2.0.0/my-bsn-2.0.0.jar").exists());

        offline.apply(patch200);
        startup = readFully(new File(karaf, "etc/startup.properties"));
        overrides = readFully(new File(karaf, "etc/overrides.properties"));

        assertEquals("foo/my-bsn/1.4.0/my-bsn-1.4.0.jar=1", startup.trim());
        assertEquals("mvn:foo/my-bsn/1.4.0;range=[1.3.0,1.5.0)\nmvn:foo/my-bsn/2.0.0", overrides.trim());
        assertFalse(new File(karaf, "system/foo/my-bsn/1.3.2/my-bsn-1.3.2.jar").exists());
        assertTrue(new File(karaf, "system/foo/my-bsn/1.4.0/my-bsn-1.4.0.jar").exists());
        assertTrue(new File(karaf, "system/foo/my-bsn/2.0.0/my-bsn-2.0.0.jar").exists());
    }

    @Test
    public void testLoadWithoutRanges() throws IOException {
        BundleContext bundleContext = createMock(BundleContext.class);
        Bundle sysBundle = createMock(Bundle.class);
        BundleContext sysBundleContext = createMock(BundleContext.class);
        Bundle bundle = createMock(Bundle.class);
        Bundle bundle2 = createMock(Bundle.class);
        FrameworkWiring wiring = createMock(FrameworkWiring.class);

        //
        // Create a new service, download a patch
        //
        expect(bundleContext.getBundle(0)).andReturn(sysBundle);
        expect(sysBundle.getBundleContext()).andReturn(sysBundleContext);
        expect(sysBundleContext.getProperty("fuse.patch.location"))
                .andReturn(storage.toString()).anyTimes();
        replay(sysBundleContext, sysBundle, bundleContext, bundle);

        ServiceImpl service = new ServiceImpl(bundleContext);

        Patch patch = ServiceImpl.doLoad(service, getClass().getClassLoader().getResourceAsStream("test1.patch"));
        assertEquals(2, patch.getBundles().size());
    }

    @Test
    public void testLoadWithRanges() throws IOException {
        BundleContext bundleContext = createMock(BundleContext.class);
        Bundle sysBundle = createMock(Bundle.class);
        BundleContext sysBundleContext = createMock(BundleContext.class);
        Bundle bundle = createMock(Bundle.class);

        //
        // Create a new service, download a patch
        //
        expect(bundleContext.getBundle(0)).andReturn(sysBundle);
        expect(sysBundle.getBundleContext()).andReturn(sysBundleContext);
        expect(sysBundleContext.getProperty("fuse.patch.location"))
                .andReturn(storage.toString()).anyTimes();
        replay(sysBundleContext, sysBundle, bundleContext, bundle);

        ServiceImpl service = new ServiceImpl(bundleContext);

        Patch patch = ServiceImpl.doLoad(service, getClass().getClassLoader().getResourceAsStream("test2.patch"));
        assertEquals(2, patch.getBundles().size());
        assertEquals("[1.0.0,2.0.0)", patch.getVersionRange("mvn:org.fusesource.test/test1/1.0.0"));
        assertNull(patch.getVersionRange("mvn:org.fusesource.test/test2/1.0.0"));
    }


    @Test
    public void testSymbolicNameStrip() {
        Assert.assertEquals("my.bundle", stripSymbolicName("my.bundle"));
        Assert.assertEquals("my.bundle", stripSymbolicName("my.bundle;singleton:=true"));
        Assert.assertEquals("my.bundle", stripSymbolicName("my.bundle;blueprint.graceperiod:=false;"));
        Assert.assertEquals("my.bundle", stripSymbolicName("my.bundle;blueprint.graceperiod:=false; blueprint.timeout=10000;"));
    }
    
    @Test
    public void testPatch() throws Exception {
        BundleContext bundleContext = createMock(BundleContext.class);
        Bundle sysBundle = createMock(Bundle.class);
        BundleContext sysBundleContext = createMock(BundleContext.class);
        Bundle bundle = createMock(Bundle.class);
        Bundle bundle2 = createMock(Bundle.class);
        FrameworkWiring wiring = createMock(FrameworkWiring.class);

        //
        // Create a new service, download a patch
        //
        expect(bundleContext.getBundle(0)).andReturn(sysBundle);
        expect(sysBundle.getBundleContext()).andReturn(sysBundleContext);
        expect(sysBundleContext.getProperty("fuse.patch.location"))
                .andReturn(storage.toString()).anyTimes();
        replay(sysBundleContext, sysBundle, bundleContext, bundle);

        ServiceImpl service = new ServiceImpl(bundleContext);
        
        try {
            service.download(new URL("file:" + storage + "/temp/f00.zip"));
            fail("Should have thrown exception on non existant patch file.");
        } catch (Exception e) {        	
        }        
        
        Iterable<Patch> patches = service.download(patch132.toURI().toURL());
        assertNotNull(patches);
        Iterator<Patch> it = patches.iterator();
        assertTrue( it.hasNext() );
        Patch patch = it.next();
        assertNotNull( patch );
        assertEquals("patch-1.3.2", patch.getId());
        assertNotNull(patch.getBundles());
        assertEquals(1, patch.getBundles().size());
        Iterator<String> itb = patch.getBundles().iterator();
        assertEquals("mvn:foo/my-bsn/1.3.2", itb.next());
        assertNull(patch.getResult());
        verify(sysBundleContext, sysBundle, bundleContext, bundle);

        //
        // Simulate the patch
        //

        reset(sysBundleContext, sysBundle, bundleContext, bundle);
        
        expect(sysBundleContext.getBundles()).andReturn(new Bundle[] { bundle });
        expect(bundle.getSymbolicName()).andReturn("my-bsn").anyTimes();
        expect(bundle.getVersion()).andReturn(new Version("1.3.1")).anyTimes();
        expect(bundle.getLocation()).andReturn("location");
        expect(bundle.getBundleId()).andReturn(123L);
        replay(sysBundleContext, sysBundle, bundleContext, bundle);
        
        Result result = patch.simulate();
        assertNotNull( result );
        assertNull( patch.getResult() ); 
        assertTrue(result.isSimulation());

        verify(sysBundleContext, sysBundle, bundleContext, bundle);

        //
        // Recreate a new service and verify the downloaded patch is still available
        //

        reset(sysBundleContext, sysBundle, bundleContext, bundle);
        expect(bundleContext.getBundle(0)).andReturn(sysBundle);
        expect(sysBundle.getBundleContext()).andReturn(sysBundleContext);
        expect(sysBundleContext.getProperty("fuse.patch.location"))
                .andReturn(storage.toString()).anyTimes();
        replay(sysBundleContext, sysBundle, bundleContext, bundle);

        service = new ServiceImpl(bundleContext);
        patches = service.getPatches();
        assertNotNull(patches);
        it = patches.iterator();
        assertTrue( it.hasNext() );
        patch = it.next();
        assertNotNull( patch );
        assertEquals("patch-1.3.2", patch.getId());
        assertNotNull(patch.getBundles());
        assertEquals(1, patch.getBundles().size());
        itb = patch.getBundles().iterator();
        assertEquals("mvn:foo/my-bsn/1.3.2", itb.next());
        assertNull(patch.getResult());
        verify(sysBundleContext, sysBundle, bundleContext, bundle);

        // 
        // Install the patch
        //
        
        reset(sysBundleContext, sysBundle, bundleContext, bundle);

        expect(sysBundleContext.getBundles()).andReturn(new Bundle[] { bundle });
        expect(bundle.getSymbolicName()).andReturn("my-bsn").anyTimes();
        expect(bundle.getVersion()).andReturn(new Version("1.3.1")).anyTimes();
        expect(bundle.getLocation()).andReturn("location");
        expect(bundle.getHeaders()).andReturn(new Hashtable()).anyTimes();
        expect(bundle.getBundleId()).andReturn(123L);
        bundle.update(EasyMock.<InputStream>anyObject());
        expect(sysBundleContext.getBundles()).andReturn(new Bundle[] { bundle });
        expect(bundle.getState()).andReturn(Bundle.INSTALLED).anyTimes();
        expect(bundle.getRegisteredServices()).andReturn(null);
        expect(sysBundleContext.getBundle(0)).andReturn(sysBundle);
        expect(sysBundle.adapt(FrameworkWiring.class)).andReturn(wiring);
        bundle.start();
        wiring.refreshBundles(eq(asSet(bundle)), anyObject(FrameworkListener[].class));
        expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                for (FrameworkListener l : (FrameworkListener[]) (EasyMock.getCurrentArguments()[1])) {
                    l.frameworkEvent(null);
                }
                return null;
            }
        });
        replay(sysBundleContext, sysBundle, bundleContext, bundle, bundle2, wiring);

        result = patch.install();
        assertNotNull( result );
        assertSame( result, patch.getResult() );
        assertFalse(patch.getResult().isSimulation());

        verify(sysBundleContext, sysBundle, bundleContext, bundle, wiring);

        //
        // Recreate a new service and verify the downloaded patch is still available and installed
        //

        reset(sysBundleContext, sysBundle, bundleContext, bundle);
        expect(bundleContext.getBundle(0)).andReturn(sysBundle);
        expect(sysBundle.getBundleContext()).andReturn(sysBundleContext);
        expect(sysBundleContext.getProperty("fuse.patch.location"))
                .andReturn(storage.toString()).anyTimes();
        replay(sysBundleContext, sysBundle, bundleContext, bundle);

        service = new ServiceImpl(bundleContext);
        patches = service.getPatches();
        assertNotNull(patches);
        it = patches.iterator();
        assertTrue( it.hasNext() );
        patch = it.next();
        assertNotNull( patch );
        assertEquals("patch-1.3.2", patch.getId());
        assertNotNull(patch.getBundles());
        assertEquals(1, patch.getBundles().size());
        itb = patch.getBundles().iterator();
        assertEquals("mvn:foo/my-bsn/1.3.2", itb.next());
        assertNotNull(patch.getResult());
        verify(sysBundleContext, sysBundle, bundleContext, bundle);
    }

    @Test
    public void testPatchWithVersionRanges() throws Exception {
        BundleContext bundleContext = createMock(BundleContext.class);
        Bundle sysBundle = createMock(Bundle.class);
        BundleContext sysBundleContext = createMock(BundleContext.class);
        Bundle bundle = createMock(Bundle.class);
        Bundle bundle2 = createMock(Bundle.class);
        FrameworkWiring wiring = createMock(FrameworkWiring.class);

        //
        // Create a new service, download a patch
        //
        expect(bundleContext.getBundle(0)).andReturn(sysBundle);
        expect(sysBundle.getBundleContext()).andReturn(sysBundleContext);
        expect(sysBundleContext.getProperty("fuse.patch.location"))
                .andReturn(storage.toString()).anyTimes();
        replay(sysBundleContext, sysBundle, bundleContext, bundle);

        ServiceImpl service = new ServiceImpl(bundleContext);
        Iterable<Patch> patches = service.download(patch140.toURI().toURL());
        assertNotNull(patches);
        Iterator<Patch> it = patches.iterator();
        assertTrue( it.hasNext() );
        Patch patch = it.next();
        assertNotNull( patch );
        assertEquals("patch-1.4.0", patch.getId());
        assertNotNull(patch.getBundles());
        assertEquals(1, patch.getBundles().size());
        Iterator<String> itb = patch.getBundles().iterator();
        assertEquals("mvn:foo/my-bsn/1.4.0", itb.next());
        assertNull(patch.getResult());
        verify(sysBundleContext, sysBundle, bundleContext, bundle);

        //
        // Simulate the patch
        //
        reset(sysBundleContext, sysBundle, bundleContext, bundle);

        expect(sysBundleContext.getBundles()).andReturn(new Bundle[] { bundle });
        expect(bundle.getSymbolicName()).andReturn("my-bsn").anyTimes();
        expect(bundle.getVersion()).andReturn(new Version("1.3.1")).anyTimes();
        expect(bundle.getLocation()).andReturn("location");
        expect(bundle.getBundleId()).andReturn(123L);
        replay(sysBundleContext, sysBundle, bundleContext, bundle);

        Result result = patch.simulate();
        assertNotNull( result );
        assertNull( patch.getResult() );
        assertEquals(1, result.getUpdates().size());
        assertTrue(result.isSimulation());
    }

    private void generateData() throws Exception {
        karaf = new File(baseDir, "karaf");
        delete(karaf);
        karaf.mkdirs();
        new File(karaf, "etc").mkdir();
        new File(karaf, "etc/startup.properties").createNewFile();
        System.setProperty("karaf.base", karaf.getAbsolutePath());

        storage = new File(baseDir, "storage");
        delete(storage);
        storage.mkdirs();

        bundlev131 = createBundle("my-bsn", "1.3.1");
        bundlev132 = createBundle("my-bsn;directive1:=true; directve2:=1000", "1.3.2");
        bundlev140 = createBundle("my-bsn", "1.4.0");
        bundlev200 = createBundle("my-bsn", "2.0.0");

        patch132 = createPatch("patch-1.3.2", bundlev132, "mvn:foo/my-bsn/1.3.2");
        patch140 = createPatch("patch-1.4.0", bundlev140, "mvn:foo/my-bsn/1.4.0", "[1.3.0,1.5.0)");
        patch200 = createPatch("patch-2.0.0", bundlev140, "mvn:foo/my-bsn/2.0.0");
    }

    private File createPatch(String id, File bundle, String mvnUrl) throws Exception {
        return createPatch(id, bundle, mvnUrl, null);
    }

    private File createPatch(String id, File bundle, String mvnUrl, String range) throws Exception {
        File patchFile = new File(storage, "temp/" + id + ".zip");
        File pd = new File(storage, "temp/" + id + "/" + id + ".patch");
        pd.getParentFile().mkdirs();
        Properties props = new Properties();
        props.put("id", id);
        props.put("bundle.count", "1");
        props.put("bundle.0", mvnUrl);
        if (range != null) {
            props.put("bundle.0.range", range);
        }
        FileOutputStream fos = new FileOutputStream(pd);
        props.store(fos, null);
        fos.close();
        File bf = new File(storage, "temp/" + id + "/repository/" + Offline.mvnurlToArtifact(mvnUrl, true).getPath());
        bf.getParentFile().mkdirs();
        copy(new FileInputStream(bundle), new FileOutputStream(bf));
        fos = new FileOutputStream(patchFile);
        jarDir(pd.getParentFile(), fos);
        fos.close();
        return patchFile;
    }

    private File createBundle(String bundleSymbolicName, String version) throws Exception {
        File jar = new File(storage, "temp/" + stripSymbolicName(bundleSymbolicName) + "-" + version + ".jar");
        File man = new File(storage, "temp/" + stripSymbolicName(bundleSymbolicName) + "-" + version + "/META-INF/MANIFEST.MF");
        man.getParentFile().mkdirs();
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Manifest-Version", "1.0");
        mf.getMainAttributes().putValue("Bundle-ManifestVersion", "2");
        mf.getMainAttributes().putValue("Bundle-SymbolicName", bundleSymbolicName);
        mf.getMainAttributes().putValue("Bundle-Version", version);
        FileOutputStream fos = new FileOutputStream(man);
        mf.write(fos);
        fos.close();
        fos = new FileOutputStream(jar);
        jarDir(man.getParentFile().getParentFile(), fos);
        fos.close();
        return jar;
    }

    private <T> Set<T> asSet(T... objects) {
        HashSet<T> set = new HashSet<T>();
        for (T t : objects) {
            set.add(t);
        }
        return set;
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete( child );
            }
            file.delete();
        } else if (file.isFile()) {
            file.delete();
        }
    }

    private URL getZippedTestDir(String name) throws IOException {
        File f2 = new File(baseDir, name + ".jar");
        OutputStream os = new FileOutputStream(f2);
        jarDir(new File(baseDir, name), os);
        os.close();
        return f2.toURI().toURL();
    }


    public static void jarDir(File directory, OutputStream os) throws IOException
    {
        // create a ZipOutputStream to zip the data to
        JarOutputStream zos = new JarOutputStream(os);
        zos.setLevel(Deflater.NO_COMPRESSION);
        String path = "";
        File manFile = new File(directory, JarFile.MANIFEST_NAME);
        if (manFile.exists())
        {
            byte[] readBuffer = new byte[8192];
            FileInputStream fis = new FileInputStream(manFile);
            try
            {
                ZipEntry anEntry = new ZipEntry(JarFile.MANIFEST_NAME);
                zos.putNextEntry(anEntry);
                int bytesIn = fis.read(readBuffer);
                while (bytesIn != -1)
                {
                    zos.write(readBuffer, 0, bytesIn);
                    bytesIn = fis.read(readBuffer);
                }
            }
            finally
            {
                fis.close();
            }
            zos.closeEntry();
        }
        zipDir(directory, zos, path, Collections.singleton(JarFile.MANIFEST_NAME));
        // close the stream
        zos.close();
    }

    public static void zipDir(File directory, ZipOutputStream zos, String path, Set/* <String> */ exclusions) throws IOException
    {
        // get a listing of the directory content
        File[] dirList = directory.listFiles();
        byte[] readBuffer = new byte[8192];
        int bytesIn = 0;
        // loop through dirList, and zip the files
        for (int i = 0; i < dirList.length; i++)
        {
            File f = dirList[i];
            if (f.isDirectory())
            {
                String prefix = path + f.getName() + "/";
                zos.putNextEntry(new ZipEntry(prefix));
                zipDir(f, zos, prefix, exclusions);
                continue;
            }
            String entry = path + f.getName();
            if (!exclusions.contains(entry))
            {
                FileInputStream fis = new FileInputStream(f);
                try
                {
                    ZipEntry anEntry = new ZipEntry(entry);
                    zos.putNextEntry(anEntry);
                    bytesIn = fis.read(readBuffer);
                    while (bytesIn != -1)
                    {
                        zos.write(readBuffer, 0, bytesIn);
                        bytesIn = fis.read(readBuffer);
                    }
                }
                finally
                {
                    fis.close();
                }
            }
        }
    }

    public class CustomBundleURLStreamHandlerFactory implements
            URLStreamHandlerFactory {
        private static final String MVN_URI_PREFIX = "mvn";

        public URLStreamHandler createURLStreamHandler(String protocol) {
            if (protocol.equals(MVN_URI_PREFIX)) {
                return new MvnHandler();
            } else {
                return null;
            }
        }

    }

    public class MvnHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            if (u.toString().equals("mvn:foo/my-bsn/1.3.1")) {
                return bundlev131.toURI().toURL().openConnection();
            }
            if (u.toString().equals("mvn:foo/my-bsn/1.3.2")) {
                return bundlev132.toURI().toURL().openConnection();
            }
            if (u.toString().equals("mvn:foo/my-bsn/1.4.0")) {
                return bundlev140.toURI().toURL().openConnection();
            }
            if (u.toString().equals("mvn:foo/my-bsn/2.0.0")) {
                return bundlev200.toURI().toURL().openConnection();
            }
            throw new IllegalArgumentException(u.toString());
        }
    }

}
