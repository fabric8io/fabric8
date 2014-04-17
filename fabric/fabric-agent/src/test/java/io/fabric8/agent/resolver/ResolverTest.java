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
package io.fabric8.agent.resolver;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

import aQute.lib.osgi.Macro;
import aQute.lib.osgi.Processor;
import org.apache.felix.framework.Felix;
import org.apache.karaf.features.Repository;
import io.fabric8.agent.DeploymentBuilder;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.mvn.MavenConfigurationImpl;
import io.fabric8.agent.mvn.MavenSettingsImpl;
import io.fabric8.agent.mvn.PropertiesPropertyResolver;
import io.fabric8.agent.utils.AgentUtils;
import org.junit.Test;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;

import static io.fabric8.agent.resolver.UriNamespace.getUri;
import static org.junit.Assert.assertEquals;

/**
 */
public class ResolverTest {

    @Test
    public void testResolve() throws Exception {
        System.setProperty("karaf.data", new File("target/karaf/data").getAbsolutePath());
        System.setProperty("karaf.home", new File("target/karaf").getAbsolutePath());
        String home = System.getProperty("user.home");
        Properties properties = new Properties();
        properties.setProperty("mvn.localRepository", home+"/.m2/repository/@snapshots");
        properties.setProperty("mvn.repositories", "http://repo1.maven.org/maven2/,https://repo.fusesource.com/nexus/content/repositories/ea");
        PropertiesPropertyResolver propertyResolver = new PropertiesPropertyResolver(properties);
        MavenConfigurationImpl mavenConfiguration = new MavenConfigurationImpl(propertyResolver, "mvn");
        mavenConfiguration.setSettings(new MavenSettingsImpl(new URL("file:"+home+"/.m2/settings.xml")));

        DownloadManager manager = new DownloadManager(mavenConfiguration, Executors.newFixedThreadPool(2));

        Map<URI, Repository> repositories = new HashMap<URI, Repository>();
        AgentUtils.addRepository(manager, repositories, URI.create("mvn:org.apache.karaf.assemblies.features/standard/" + System.getProperty("karaf-version") + "/xml/features"));

        DeploymentBuilder builder = new DeploymentBuilder(manager, null, repositories.values(), 0);

        builder.download(new HashSet<String>(Arrays.asList("karaf-framework", "ssh")),
                         Collections.<String>emptySet(),
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
