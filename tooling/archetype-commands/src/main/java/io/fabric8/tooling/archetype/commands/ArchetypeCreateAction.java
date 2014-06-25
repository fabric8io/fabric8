/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.tooling.archetype.commands;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.fabric8.agent.download.DownloadFuture;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.FutureListener;
import io.fabric8.agent.mvn.MavenConfigurationImpl;
import io.fabric8.agent.mvn.MavenSettingsImpl;
import io.fabric8.agent.mvn.PropertiesPropertyResolver;
import io.fabric8.tooling.archetype.ArchetypeService;
import io.fabric8.tooling.archetype.catalog.Archetype;
import io.fabric8.tooling.archetype.generator.ArchetypeHelper;
import io.fabric8.utils.shell.ShellUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ArchetypeInfo.FUNCTION_VALUE, scope = ArchetypeCreate.SCOPE_VALUE, description = ArchetypeCreate.DESCRIPTION)
public class ArchetypeCreateAction extends AbstractAction {

    @Argument(index = 0, name = "archetype", description = "Archetype coordinates", required = true, multiValued = false)
    private String archetypeGAV;

    @Argument(index = 1, name = "target", description = "Target directory where the project will be generated", required = true, multiValued = false)
    private File target;

    private final ArchetypeService archetypeService;

    public ArchetypeCreateAction(ArchetypeService archetypeService) {
        this.archetypeService = archetypeService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Archetype archetype = archetypeService.getArchetype(archetypeGAV);
        if (archetype != null) {
            System.out.println(String.format("Generating %s:%s in %s", archetype.groupId, archetype.artifactId, target.getCanonicalPath()));
            InputStream archetypeInputStream = fetchArchetype(archetype);
            if (archetypeInputStream == null) {
                System.err.println("No archetype found for \"" + archetypeGAV + "\" coordinates");
                return null;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(archetypeInputStream, baos);
            IOUtils.closeQuietly(archetypeInputStream);
            InputStream stream = new ByteArrayInputStream(baos.toByteArray());

            String defaultGroupId = "io.fabric8";
            String defaultArtifactId = archetype.artifactId + "-example";
            String defaultVersion = "1.0-SNAPSHOT";
            System.out.println("----- Configure archetype -----");
            String groupId = ShellUtils.readLine(session, String.format("Define value for property 'groupId' (%s):", defaultGroupId), false);
            String artifactId = ShellUtils.readLine(session, String.format("Define value for property 'artifactId' (%s):", defaultArtifactId), false);
            String version = ShellUtils.readLine(session, String.format("Define value for property 'version' (%s):", defaultVersion), false);

            groupId = groupId == null || groupId.trim().equals("") ? defaultGroupId : groupId;
            artifactId = artifactId == null || artifactId.trim().equals("") ? defaultArtifactId : artifactId;
            version = version == null || version.trim().equals("") ? defaultVersion : version;

            String defaultPackageName = (groupId + "." + artifactId).replaceAll("-", ".");
            String packageName = ShellUtils.readLine(session, String.format("Define value for property 'package' (%s):", defaultPackageName), false);
            packageName = packageName == null || packageName.trim().equals("") ? defaultPackageName : packageName;

            ArchetypeHelper helper = new ArchetypeHelper(stream, target, groupId, artifactId, version);
            helper.setPackageName(packageName);

            Map<String, String> properties = helper.parseProperties();
            // ask for replacement properties suggesting the defaults
            if (!properties.isEmpty()) {
                System.out.println("----- Configure additional properties -----");
                for (String key : properties.keySet()) {
                    String p = ShellUtils.readLine(session, String.format("Define value for property '%s' (%s):", key, properties.get(key)), false);
                    p = p == null || p.trim().equals("") ? properties.get(key) : p;
                    properties.put(key, p);
                }
            }
            helper.setOverrideProperties(properties);
            stream.reset();
            helper.execute();
        } else {
            System.err.println("No archetype found for \"" + archetypeGAV + "\" coordinates");
        }
        return null;
    }

    /**
     * Fetches archetype from the configured repositories
     * TODO: make this code available to hawt.io/JMX too
     *
     * @param archetype
     * @return
     */
    private InputStream fetchArchetype(Archetype archetype) throws IOException {
        MavenConfigurationImpl config = new MavenConfigurationImpl(new PropertiesPropertyResolver(System.getProperties()), "org.ops4j.pax.url.mvn");
        config.setSettings(new MavenSettingsImpl(config.getSettingsFileUrl(), config.useFallbackRepositories()));
        DownloadManager dm = new DownloadManager(config, Executors.newSingleThreadExecutor());

        final CountDownLatch latch = new CountDownLatch(1);
        DownloadFuture df = dm.download(String.format("mvn:%s/%s/%s", archetype.groupId, archetype.artifactId, archetype.version));
        df.addListener(new FutureListener<DownloadFuture>() {
            @Override
            public void operationComplete(DownloadFuture future) {
                latch.countDown();
            }
        });

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Failed to download " + archetype);
            throw new IOException(e.getMessage(), e);
        }
        System.out.println("Downloaded archetype (" + df.getFile() + ")");

        return new FileInputStream(df.getFile());
    }

}
