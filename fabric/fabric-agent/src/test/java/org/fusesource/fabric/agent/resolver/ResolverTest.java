package org.fusesource.fabric.agent.resolver;

import org.apache.felix.framework.Felix;
import org.apache.felix.resolver.ResolverImpl;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.fusesource.fabric.agent.download.DownloadManager;
import org.fusesource.fabric.agent.mvn.MavenConfigurationImpl;
import org.fusesource.fabric.agent.mvn.MavenSettingsImpl;
import org.fusesource.fabric.agent.mvn.PropertiesPropertyResolver;
import org.fusesource.fabric.agent.utils.AgentUtils;
import org.fusesource.fabric.fab.osgi.FabBundleInfo;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.fusesource.fabric.agent.utils.AgentUtils.downloadBundles;

/**
 */
public class ResolverTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResolverTest.class);

    @Test
    public void testResolve() throws Exception {
        System.setProperty("karaf.data", new File("target/karaf/data").getAbsolutePath());
        System.setProperty("karaf.home", new File("target/karaf").getAbsolutePath());



        Properties properties = new Properties();
        properties.setProperty("mvn.localRepository", "/Users/gnodet/.m2/repository/@snapshots");
        properties.setProperty("mvn.repositories", "http://repo1.maven.org/maven2/,http://repo.fusesource.com/nexus/content/repositories/ea");
        PropertiesPropertyResolver propertyResolver = new PropertiesPropertyResolver(properties);
        MavenConfigurationImpl mavenConfiguration = new MavenConfigurationImpl(propertyResolver, "mvn");
        mavenConfiguration.setSettings(new MavenSettingsImpl(new URL("file:/Users/gnodet/.m2/settings.xml")));
        DownloadManager manager = new DownloadManager(mavenConfiguration, Executors.newFixedThreadPool(2));

        Map<URI, Repository> repositories = new HashMap<URI, Repository>();
        AgentUtils.addRepository(manager, repositories, URI.create("mvn:org.apache.karaf.assemblies.features/standard/2.3.0.redhat-610-SNAPSHOT/xml/features"));

        Set<Feature> allFeatures = new HashSet<Feature>();
        Repository repo = repositories.values().iterator().next();
        allFeatures.addAll(Arrays.asList(repo.getFeatures()));

        Map<String, File> downloads = downloadBundles(manager, allFeatures, Collections.<String>emptySet(), Collections.<String>emptySet());

        Map<String, Resource> locToRes = new HashMap<String, Resource>();
        List<Requirement> reqs = new ArrayList<Requirement>();
        List<Resource> ress = new ArrayList<Resource>();
        List<Resource> deploy = new ArrayList<Resource>();
        Map<Object, BundleInfo> infos = new HashMap<Object, BundleInfo>();
        for (Feature feature : allFeatures) {
            for (BundleInfo bundleInfo : feature.getBundles()) {
                try {
                    Resource res = createResource(bundleInfo.getLocation(), downloads, Collections.<String, FabBundleInfo>emptyMap());
                    if (res == null) {
                        throw new IllegalArgumentException("Unable to build OBR representation for bundle " + bundleInfo.getLocation());
                    }
                    locToRes.put(bundleInfo.getLocation(), res);
                    ress.add(res);
                    infos.put(res, bundleInfo);
                } catch (MalformedURLException e) {
                    List<Requirement> reqList = parseRequirement(bundleInfo.getLocation());
                    for (Requirement req : reqList) {
                        reqs.add(req);
                        infos.put(req, bundleInfo);
                    }
                }
            }
        }


        // System bundle
        properties = new Properties();
        properties.setProperty("org.osgi.framework.system.packages.extra", "org.apache.karaf.jaas.boot;version=\"2.3.0.redhat-610-SNAPSHOT\",org.apache.karaf.jaas.boot.principal;version=\"2.3.0.redhat-610-SNAPSHOT\"");
        properties.setProperty("org.osgi.framework.system.capabilities.extra",
                "service-reference;effective:=active;objectClass=org.osgi.service.packageadmin.PackageAdmin," +
                "service-reference;effective:=active;objectClass=org.osgi.service.startlevel.StartLevel," +
                "service-reference;effective:=active;objectClass=org.osgi.service.url.URLHandlers");

        Framework felix = new Felix(properties);
        ress.add(felix.adapt(BundleRevisions.class).getRevisions().iterator().next());

        Map<Feature, Resource> featureResourceMap = new HashMap<Feature, Resource>();
        for (Feature feature : repo.getFeatures()) {
            Resource resf = FeatureResource.build(feature, locToRes);
            ress.add(resf);
            featureResourceMap.put(feature, resf);
        }

        Map<String, String> types = new HashMap<String, String>();
        types.put("ResourceImpl",             "bundle ");
        types.put("FeatureResource",          "feature");
        types.put("ExtensionManagerRevision", "system ");

        for (Feature feature : repo.getFeatures()) {
            Set<Resource> mandatory = new HashSet<Resource>();
            Set<Resource> optional = new HashSet<Resource>();
            mandatory.add(featureResourceMap.get(feature));

            ResolverImpl resolver = new ResolverImpl(new org.apache.felix.resolver.Logger(org.apache.felix.resolver.Logger.LOG_DEBUG));
            ResolveContext context = new ResolveContextImpl(mandatory, optional, ress, false);

            System.out.println("Resolution for feature " + feature.getName());
            try {
                Map<Resource, List<Wire>> wires = resolver.resolve(context);
                for (Map.Entry<Resource, List<Wire>> entry : wires.entrySet()) {
                    System.out.println("    Resource: " + types.get(entry.getKey().getClass().getSimpleName()) + ": " + entry.getKey());
                    for (Wire wire : entry.getValue()) {
    //                System.out.println("    " + wire);
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace(System.out);
            }
            System.out.println();
        }

    }


    private Resource createResource(String uri, Map<String, File> urls, Map<String, FabBundleInfo> infos) throws Exception {
        Attributes attributes = getAttributes(uri, urls, infos);
        Map<String, String> headers = new HashMap<String, String>();
        for (Map.Entry attr : attributes.entrySet()) {
            headers.put(attr.getKey().toString(), attr.getValue().toString());
        }
        try {
            Resource res = ResourceBuilder.build(null, uri, headers);
            return res;
        } catch (BundleException e) {
            throw new Exception("Unable to create Resource for bundle " + uri, e);
        }
    }

    private static Attributes getAttributes(String uri, Map<String, File> urls, Map<String, FabBundleInfo> infos) throws Exception {
        InputStream is = getBundleInputStream(uri, urls, infos);
        byte[] man = loadEntry(is, JarFile.MANIFEST_NAME);
        if (man == null) {
            throw new IllegalArgumentException("The specified url is not a valid jar (can't read manifest): " + uri);
        }
        Manifest manifest = new Manifest(new ByteArrayInputStream(man));
        return manifest.getMainAttributes();
    }

    private static InputStream getBundleInputStream(String uri, Map<String, File> downloads, Map<String, FabBundleInfo> infos) throws Exception {
        InputStream is;
        File file;
        FabBundleInfo info;
        if ((file = downloads.get(uri)) != null) {
            is = new FileInputStream(file);
        } else if ((info = infos.get(uri)) != null) {
            is = info.getInputStream();
        } else {
            LOGGER.warn("Bundle " + uri + " not found in the downloads, using direct input stream instead");
            is = new URL(uri).openStream();
        }
        return is;
    }

    private static byte[] loadEntry(InputStream is, String name) throws IOException {
        try {
            ZipInputStream jis = new ZipInputStream(is);
            for (ZipEntry e = jis.getNextEntry(); e != null; e = jis.getNextEntry()) {
                if (name.equalsIgnoreCase(e.getName())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int n;
                    while ((n = jis.read(buf, 0, buf.length)) > 0) {
                        baos.write(buf, 0, n);
                    }
                    return baos.toByteArray();
                }
            }
        } finally {
            is.close();
        }
        return null;
    }

    private static List<Requirement> parseRequirement(String req) throws BundleException {
        return ResourceBuilder.parseRequirement(null, null, req);
    }
}
