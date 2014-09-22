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
import java.util.Iterator;
import java.util.List;
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
import io.fabric8.common.util.Strings;
import io.fabric8.tooling.archetype.ArchetypeService;
import io.fabric8.tooling.archetype.catalog.Archetype;
import io.fabric8.tooling.archetype.generator.ArchetypeHelper;
import io.fabric8.utils.shell.ShellUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

import static io.fabric8.common.util.Strings.isNotBlank;
import static io.fabric8.common.util.Strings.isNullOrBlank;

@Command(name = ArchetypeGenerate.FUNCTION_VALUE, scope = ArchetypeGenerate.SCOPE_VALUE, description = ArchetypeGenerate.DESCRIPTION, detailedDescription = "classpath:archetypeGenerate.txt")
public class ArchetypeGenerateAction extends AbstractAction {

    @Argument(index = 0, name = "archetype", description = "Archetype id, or coordinate, or filter", required = false, multiValued = false)
    private String archetypeOrFilter;

    @Option(name = "-d", aliases = "--directory", description = "To use a specific directory as base for where the project is created (by default the current workspace location is used)", required = false, multiValued = false)
    private String directory;

    private final ArchetypeService archetypeService;

    public ArchetypeGenerateAction(ArchetypeService archetypeService) {
        this.archetypeService = archetypeService;
    }

    @Override
    protected Object doExecute() throws Exception {
        // if no directory then use workspace
        if (directory == null) {
            // must have a workspace location configured
            Preferences preferences = Preferences.userNodeForPackage(getClass());
            String location = preferences.get(ArchetypeWorkspace.PREFERENCE_WORKSPACE, null);
            if (location == null) {
                System.out.println("No workspace location has been set.");
                System.out.println("Use the archetype-workspace command to set a workspace first.");
                System.out.println("");
                return null;
            } else {
                System.out.println("Using current workspace: " + location);
                directory = location;
            }
        } else {
            System.out.println("Using directory as workspace: " + directory);
        }

        File target = new File(directory);

        // make sure the directory exists, auto-creating if missing
        if (!target.exists()) {
            target.mkdirs();
        }
        if (!target.exists() || !target.isDirectory()) {
            System.err.println("Workspace does not exists or is not a directory: " + directory);
            return null;
        }

        Archetype archetype = null;

        // try artifact first
        if (!isNullOrBlank(archetypeOrFilter)) {
            archetype = archetypeService.getArchetypeByArtifact(archetypeOrFilter);
            if (archetype == null) {
                // then by coordinate
                archetype = archetypeService.getArchetype(archetypeOrFilter);
            }
        }

        // no archetype yet so present a list where the user can select
        while (archetype == null) {
            List<Archetype> archetypes = archetypeService.listArchetypes(archetypeOrFilter, true);

            System.out.println("Choose archetype:");
            Iterator<Archetype> it = archetypes.iterator();
            int i = 0;
            while (it.hasNext()) {
                Archetype select = it.next();
                System.out.println(String.format("%4d: -> %-50s %s", ++i, select.artifactId, select.description));
            }

            boolean choosing = true;
            while (choosing) {

                // default select last
                String choose = ShellUtils.readLine(session, String.format("Choose a number or apply filter (case insensitive): %d: ", i), false);
                if (Strings.isNullOrBlank(choose)) {
                    // user pressed enter so we select the last
                    choose = "" + i;
                }

                try {
                    int no = Integer.valueOf(choose);

                    // is the number within range
                    if (no >= 1 && no <= archetypes.size()) {
                        archetype = archetypes.get(no - 1);
                        break;
                    } else {
                        System.out.println("Number " + no + " out of range. Please try again!");
                        continue;
                    }
                } catch (NumberFormatException e) {
                    // no its a filter, so we use this as filter, and show the list again
                    archetypeOrFilter = choose;
                    choosing = false;
                    archetype = null;
                }
            }
        }

        // okay we have selected an archetype now

        File archetypeFile = fetchArchetype(archetype);
        if (archetypeFile == null || !archetypeFile.exists()) {
            System.err.println("No archetype found for \"" + archetypeOrFilter + "\" coordinates");
            return null;
        }

        System.out.println("----------------------------------------------------------------------------");
        System.out.println("Using archetype: " + archetype.artifactId);

        String defaultGroupId = "io.fabric8";
        String defaultArtifactId = archetype.artifactId + "-example";
        String defaultVersion = "1.0-SNAPSHOT";
        String defaultName = archetype.name;
        String defaultDescription = isNotBlank(archetype.description) ? archetype.description : "";

        System.out.println("----- Configure archetype -----");
        String groupId = ShellUtils.readLine(session, String.format("Define value for property 'groupId' (%s): ", defaultGroupId), false);
        String artifactId = ShellUtils.readLine(session, String.format("Define value for property 'artifactId' (%s): ", defaultArtifactId), false);
        String version = ShellUtils.readLine(session,  String.format("Define value for property 'version' (%s): ", defaultVersion), false);

        groupId = isNullOrBlank(groupId) ? defaultGroupId : groupId;
        artifactId = isNullOrBlank(artifactId) ? defaultArtifactId : artifactId;
        version = isNullOrBlank(version) ? defaultVersion : version;

        String defaultPackageName = (groupId + "." + artifactId).replaceAll("-", ".");
        String packageName = ShellUtils.readLine(session, String.format("Define value for property 'package' (%s): ", defaultPackageName), false);
        // use artifact id as default directory name (maven does this also)
        String defaultDirectoryName = isNullOrBlank(artifactId) ? defaultArtifactId : artifactId;
        directory = ShellUtils.readLine(session, String.format("Define value for property 'directoryName' (%s): ", defaultDirectoryName), false);

        packageName = isNullOrBlank(packageName) ? defaultPackageName : packageName;
        directory = isNullOrBlank(directory) ? artifactId : directory;

        String name = ShellUtils.readLine(session,  String.format("Define value for property 'name' (%s): ", defaultName), false);
        String description = ShellUtils.readLine(session,  String.format("Define value for property 'description' (%s): ", defaultDescription), false);
        // use null to indicate we want out of the box description
        name = isNullOrBlank(name) ? null : name;
        description = isNullOrBlank(description) ? null : description;

        File childDir = new File(target, directory);

        ArchetypeHelper helper = new ArchetypeHelper(archetypeFile, childDir, groupId, artifactId, version, name, description);
        helper.setPackageName(packageName);

        Map<String, String> properties = helper.parseProperties();

        // if we have fabric8.profile as a property then lets configured it now, as its mandatory
        // and use artifactId as its default suggested value
        String profile = null;
        if (properties.containsKey("fabric8.profile")) {
            profile = properties.remove("fabric8.profile");
            String defaultProfile = isNullOrBlank(profile) ? artifactId : profile;
            String p = ShellUtils.readLine(session, String.format("Define value for property 'fabric8.profile' (%s): ", defaultProfile), false);
            profile = isNullOrBlank(p) ? defaultProfile : p;
        }

        // show additional properties and ask to use them as-is
        boolean mustChoose = false;
        if (!properties.isEmpty()) {

            // check if we must choose if there is an empty value or a value that has a ${ } token so we dont have a default value
            for (String value : properties.values()) {
                if (isNullOrBlank(value) || value.contains("$")) {
                    mustChoose = true;
                    break;
                }
            }

            if (!mustChoose) {
                System.out.println("----- Additional properties -----");
                for (String key : properties.keySet()) {
                    System.out.println(String.format("Using property '%s' (%s): ", key, properties.get(key)));
                }
            }

            boolean choosing = true;
            while (mustChoose || choosing) {

                String confirm = null;
                if (!mustChoose) {
                    confirm = ShellUtils.readLine(session, "Confirm additional properties configuration: (Y): ", false);
                    confirm = isNullOrBlank(confirm) ? "Y" : confirm;
                }

                if (mustChoose || !"Y".equalsIgnoreCase(confirm)) {
                    // ask for replacement properties suggesting the defaults
                    if (!properties.isEmpty()) {
                        System.out.println("----- Configure additional properties -----");
                        for (String key : properties.keySet()) {
                            String value = properties.get(key);
                            // if the value is empty or a token, then do not show any default value
                            if (isNullOrBlank(value) || value.contains("$")) {
                                value = "";
                            }
                            String p = ShellUtils.readLine(session, String.format("Define value for property '%s' (%s): ", key, value), false);
                            p = isNullOrBlank(p) ? value : p;
                            properties.put(key, p);
                        }
                    }
                    mustChoose = false;
                } else {
                    choosing = false;
                }
            }
        }

        // remover to include the profile back into properties
        if (profile != null) {
            properties.put("fabric8.profile", profile);
        }

        if (!properties.isEmpty()) {
            // set override properties
            helper.setOverrideProperties(properties);
        }

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
            for (int i = 0; i < 2 * 60 && latch.getCount() > 0; i++) {
                // dont do anything in the first 3 seconds as we likely can download it faster
                if (i > 3) {
                    if (!init) {
                        System.out.print("Downloading archetype in progress: ");
                        init = true;
                    }
                    System.out.print(".");
                }
                // only sleep 0.5 sec so we can react faster
                Thread.sleep(500);
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
