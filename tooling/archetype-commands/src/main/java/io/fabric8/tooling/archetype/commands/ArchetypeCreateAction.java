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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

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
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

import static io.fabric8.common.util.Strings.isNullOrBlank;

@Command(name = ArchetypeInfo.FUNCTION_VALUE, scope = ArchetypeCreate.SCOPE_VALUE, description = ArchetypeCreate.DESCRIPTION)
public class ArchetypeCreateAction extends AbstractAction {

    private static final String DEFAULT_TARGET = "/tmp";

    @Argument(index = 0, name = "archetype", description = "ArchetypeId or coordinate", required = true, multiValued = false)
    private String archetypeGAV;

    @Argument(index = 1, name = "target", description = "Target directory where the project will be generated in a sub directory", required = false, multiValued = false)
    private File target;

    @Argument(index = 2, name = "directoryName", description = "The sub directory name", required = false, multiValued = false)
    private String directoryName;

    private final ArchetypeService archetypeService;

    public ArchetypeCreateAction(ArchetypeService archetypeService) {
        this.archetypeService = archetypeService;
    }

    @Override
    protected Object doExecute() throws Exception {
        // try artifact first
        Archetype archetype = archetypeService.getArchetypeByArtifact(archetypeGAV);
        if (archetype == null) {
            // then by coordinate
            archetypeService.getArchetype(archetypeGAV);
        }

        if (archetype != null) {
            Preferences preferences = Preferences.userNodeForPackage(getClass());
            if (target == null) {
                target = new File(preferences.get("target", DEFAULT_TARGET));
            } else {
                preferences.put("target", target.getCanonicalPath());
            }
            File archetypeFile = fetchArchetype(archetype);
            if (archetypeFile == null || !archetypeFile.exists()) {
                System.err.println("No archetype found for \"" + archetypeGAV + "\" coordinates");
                return null;
            }

            String defaultGroupId = "io.fabric8";
            String defaultArtifactId = archetype.artifactId + "-example";
            String defaultVersion = "1.0-SNAPSHOT";

            System.out.println("----- Configure archetype -----");
            String groupId = ShellUtils.readLine(session, String.format("Define value for property 'groupId' (%s): ", defaultGroupId), false);
            String artifactId = ShellUtils.readLine(session, String.format("Define value for property 'artifactId' (%s): ", defaultArtifactId), false);
            String version = ShellUtils.readLine(session,  String.format("Define value for property 'version' (%s): ", defaultVersion), false);
            String defaultPackageName = (groupId + "." + artifactId).replaceAll("-", ".");
            String packageName = ShellUtils.readLine(session, String.format("Define value for property 'package' (%s): ", defaultPackageName), false);
            if (directoryName == null) {
                // use artifact id as default directory name (maven does this also)
                String defaultDirectoryName = isNullOrBlank(artifactId) ? defaultArtifactId : artifactId;
                directoryName = ShellUtils.readLine(session, String.format("Define value for property 'directoryName' (%s): ", defaultDirectoryName), false);
            }

            groupId = isNullOrBlank(groupId) ? defaultGroupId : groupId;
            artifactId = isNullOrBlank(artifactId) ? defaultArtifactId : artifactId;
            version = isNullOrBlank(version) ? defaultVersion : version;
            packageName = isNullOrBlank(packageName) ? defaultPackageName : packageName;
            directoryName = isNullOrBlank(directoryName) ? artifactId : directoryName;

            File childDir = new File(target, directoryName);

            ArchetypeHelper helper = new ArchetypeHelper(archetypeFile, childDir, groupId, artifactId, version);
            helper.setPackageName(packageName);

            Map<String, String> properties = helper.parseProperties();
            // ask for replacement properties suggesting the defaults
            if (!properties.isEmpty()) {
                System.out.println("----- Configure additional properties -----");
                for (String key : properties.keySet()) {
                    String p = ShellUtils.readLine(session, String.format("Define value for property '%s' (%s): ", key, properties.get(key)), false);
                    p = p == null || p.trim().equals("") ? properties.get(key) : p;
                    properties.put(key, p);
                }
            }
            helper.setOverrideProperties(properties);

            String confirm = ShellUtils.readLine(session, "Create project: (Y): ", false);
            confirm = confirm == null || confirm.trim().equals("") ? "Y" : confirm;

            if ("Y".equalsIgnoreCase(confirm)) {
                System.out.println("----------------------------------------------------------------------------");
                System.out.println(String.format("Creating project in directory: %s", childDir.getCanonicalPath()));
                helper.execute();
                System.out.println("Project created successfully");
                System.out.println("");
            } else {
                System.out.println("----------------------------------------------------------------------------");
                System.out.println("Creating project aborted!");
                System.out.println("");
            }
        } else {
            System.err.println("No archetype found for: " + archetypeGAV);
        }
        return null;
    }

    /**
     * Fetches archetype from the configured repositories
     * TODO: make this code available to hawt.io/JMX too
     */
    private File fetchArchetype(Archetype archetype) throws IOException {
        MavenConfigurationImpl config = new MavenConfigurationImpl(new PropertiesPropertyResolver(System.getProperties()), "org.ops4j.pax.url.mvn");
        config.setSettings(new MavenSettingsImpl(config.getSettingsFileUrl(), config.useFallbackRepositories()));
        DownloadManager dm = new DownloadManager(config, Executors.newSingleThreadExecutor());

        final CountDownLatch latch = new CountDownLatch(1);
        final DownloadFuture df = dm.download(String.format("mvn:%s/%s/%s", archetype.groupId, archetype.artifactId, archetype.version));
        df.addListener(new FutureListener<DownloadFuture>() {
            @Override
            public void operationComplete(DownloadFuture future) {
                latch.countDown();
            }
        });

        // wait for download
        try {
            boolean init = false;
            for (int i = 0; i < 60 && latch.getCount() > 0; i++) {
                // dont do anything in the first 3 seconds as we likely can download it faster
                if (i > 3) {
                    if (!init) {
                        System.out.print("Downloading archetype in progress: ");
                        init = true;
                    }
                    System.out.print(".");
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            System.err.println("\nFailed to download " + archetype);
            throw new IOException(e.getMessage(), e);
        }

        if (latch.getCount() == 0) {
            return df.getFile();
        } else {
            System.err.println("\nFailed to download archetype within 60 seconds: " + archetype);
            throw new IOException("Failed to download archetype within 60 seconds: " + archetype);
        }
    }

}
