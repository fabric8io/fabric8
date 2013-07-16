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
package org.fusesource.fabric.agent.resolver;

import aQute.lib.osgi.Macro;
import aQute.lib.osgi.Processor;
import org.apache.felix.framework.Felix;
import org.apache.felix.resolver.ResolverImpl;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.fusesource.common.util.Manifests;
import org.fusesource.fabric.agent.DeploymentBuilder;
import org.fusesource.fabric.agent.download.DownloadManager;
import org.fusesource.fabric.agent.mvn.MavenConfigurationImpl;
import org.fusesource.fabric.agent.mvn.MavenSettingsImpl;
import org.fusesource.fabric.agent.mvn.PropertiesPropertyResolver;
import org.fusesource.fabric.agent.utils.AgentUtils;
import org.fusesource.fabric.fab.osgi.FabBundleInfo;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleRevision;
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
import java.util.Collection;
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

import static org.fusesource.fabric.agent.resolver.UriNamespace.getUri;
import static org.fusesource.fabric.agent.utils.AgentUtils.downloadBundles;
import static org.junit.Assert.assertEquals;

/**
 */
public class ResolverTest {

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
        AgentUtils.addRepository(manager, repositories, URI.create("mvn:org.apache.karaf.assemblies.features/standard/" + System.getProperty("karaf-version") + "/xml/features"));

        DeploymentBuilder builder = new DeploymentBuilder(manager, null, repositories.values());

        builder.download(new HashSet<String>(Arrays.asList("karaf-framework", "ssh")),
                         Collections.<String>emptySet(),
                         Collections.<String>emptySet(),
                         Collections.<String>emptySet(),
                         Collections.<String>emptySet());

        properties = new Properties();
        properties.setProperty("org.osgi.framework.system.packages.extra", "org.apache.karaf.jaas.boot;version=\"2.3.0.redhat-610-SNAPSHOT\",org.apache.karaf.jaas.boot.principal;version=\"2.3.0.redhat-610-SNAPSHOT\"");
        properties.setProperty("org.osgi.framework.system.capabilities.extra",
                "service-reference;effective:=active;objectClass=org.osgi.service.packageadmin.PackageAdmin," +
                        "service-reference;effective:=active;objectClass=org.osgi.service.startlevel.StartLevel," +
                        "service-reference;effective:=active;objectClass=org.osgi.service.url.URLHandlers");
        Framework felix = new Felix(properties);
        Collection<Resource> resources = builder.resolve(felix.adapt(BundleRevision.class), false);

        for (Resource resource : resources) {
            System.out.println("Resource: " + getUri(resource));
        }

    }

    @Test
    public void testRange() throws Exception {
        Processor processor = new Processor();
        processor.setProperty("@", "1.2.3.redhat-61-SNAPSHOT");
        Macro macro = new Macro(processor);

        assertEquals("[1.2,1.3)", macro.process("${range;[==,=+)}"));
        assertEquals("[1.2.3.redhat-61-SNAPSHOT,2)", macro.process("${range;[====,+)}"));
    }

}
