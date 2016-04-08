/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.profiles.containers.karaf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import io.fabric8.profiles.containers.VelocityBasedReifier;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import static io.fabric8.profiles.ProfilesHelpers.readPropertiesFile;

/**
 * Reify Karaf container from Profiles
 */
public class KarafProjectReifier extends VelocityBasedReifier {

    private static final String KARAF_POM_VM = "/containers/karaf/pom.vm";

    private static final String REPOSITORY_PREFIX = "repository.";
    private static final String FEATURE_PREFIX = "feature.";
    private static final String BUNDLE_PREFIX = "bundle.";
    private static final String OVERRIDE_PREFIX = "override.";
    private static final String CONFIG_PREFIX = "config.";
    private static final String SYSTEM_PREFIX = "system.";
    private static final String LIB_PREFIX = "lib.";

    private static final String AGENT_PROPERTIES = "io.fabric8.agent.properties";

    public static final String CONTAINER_TYPE = "karaf";

    public KarafProjectReifier(Properties properties) {
        super(properties);
    }

    public void reify(Path target, Properties config, Path profilesDir) throws IOException {
        // reify maven project using template
        final Properties containerProperties = new Properties();
        containerProperties.putAll(defaultProperties);
        containerProperties.putAll(config);
        reifyProject(target, profilesDir, containerProperties);
    }

    private void reifyProject(Path target, final Path profilesDir, Properties properties) throws IOException {
        final File pojoFile = new File(target.toFile(), "pom.xml");
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(pojoFile));

            if( properties.getProperty("groupId")==null ) {
                properties.setProperty("groupId", "container");
            }
            if( properties.getProperty("version")==null ) {
                properties.setProperty("version", getProjectVersion());
            }
            if( properties.getProperty("description")==null ) {
                properties.setProperty("description", "");
            }

            VelocityContext context = new VelocityContext();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                context.put(entry.getKey().toString(), entry.getValue());
            }

            // read profile properties
            loadProperties(context, profilesDir);

            log.debug(String.format("Writing %s...", pojoFile));
            Template pojoTemplate = engine.getTemplate(KARAF_POM_VM);
            pojoTemplate.merge(context, writer);

            // close pojoFile
            writer.close();

            // add other resource files under src/main/resources/assembly
            final Path assemblyPath = target.resolve("src/main/resources/assembly/etc");
            log.debug(String.format("Writing resources to %s...", assemblyPath));
            Files.createDirectories(assemblyPath,
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")));
            Files.walkFileTree(profilesDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir,
                                                         final BasicFileAttributes attrs) throws IOException {
                    Files.createDirectories(assemblyPath.resolve(profilesDir.relativize(dir)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file,
                                                 final BasicFileAttributes attrs) throws IOException {

                    Path targetPath = assemblyPath.resolve(profilesDir.relativize(file));
                    String fileName = file.getFileName().toString();
                    if (AGENT_PROPERTIES.equals(fileName)) {
                        return FileVisitResult.CONTINUE;
                    }

                    // Skip over profile file that we know are not karaf config.
                    if (
                        fileName.equalsIgnoreCase("icon.svg") ||
                            fileName.equalsIgnoreCase("readme.md") ||
                            fileName.equalsIgnoreCase("summary.md") ||
                            fileName.equalsIgnoreCase("Jenkinsfile") ||
                            fileName.equalsIgnoreCase("welcome.dashboard") ||
                            fileName.endsWith("#docker") ||
                            fileName.endsWith("#openshift")
                        ) {
                        return FileVisitResult.CONTINUE;
                    }

                    String extension = extension(fileName);
                    if ("properties".equals(extension)) {

                        // Lets put auth related files in the auth dir to keep things neat.
                        boolean isAuthFile = fileName.startsWith("jmx.acl.") || fileName.startsWith("org.apache.karaf.command.acl");
                        if (isAuthFile && profilesDir.relativize(file).getParent() == null) {
                            targetPath = assemblyPath.resolve("auth").resolve(profilesDir.relativize(file));
                        }

                        // Rename .properties files to .cfg files.
                        String targetName = withoutExtension(fileName) + ".cfg";
                        targetPath = targetPath.getParent().resolve(targetName);
                    }

                    Files.createDirectories(targetPath.getParent());
                    Files.copy(file, targetPath);
                    return FileVisitResult.CONTINUE;
                }
            });

            log.debug("Done!");

        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private String getProjectVersion() {
        // TODO: perhpas use the git hash?
        return "1.0-SNAPSHOT";
    }

    static private String extension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i + 1);
        }
        return null;
    }

    static private String withoutExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(0, i);
        }
        return fileName;
    }

    private void loadProperties(VelocityContext context, Path profilesDir) throws IOException {
        Path agentProperties = profilesDir.resolve(AGENT_PROPERTIES);
        if (Files.exists(agentProperties)) {
            Properties props = readPropertiesFile(agentProperties);

            // read repositories
            context.put("repositories", getPrefixedProperty(props, REPOSITORY_PREFIX));

            // read features
            context.put("features", getPrefixedProperty(props, FEATURE_PREFIX));

            // read bundles
            context.put("bundles", getPrefixedProperty(props, BUNDLE_PREFIX));

            // read bundle overrides
            context.put("blacklistedBundles", getPrefixedProperty(props, OVERRIDE_PREFIX));

            // get config.properties
            Map<String, String> configMap = new HashMap<>();
            getPrefixedProperty(props, CONFIG_PREFIX, configMap);
            context.put("configProperties", configMap.entrySet());

            // get system.properties
            Map<String, String> systemMap = new HashMap<>();
            getPrefixedProperty(props, SYSTEM_PREFIX, systemMap);
            context.put("systemProperties", systemMap.entrySet());

            // get libraries
            context.put("libraries", getPrefixedProperty(props, LIB_PREFIX));

            // TODO add support for lib/ext (ext.xxx), lib/endorsed (endorsed.xxx) in karaf maven plugin

        } else {
            throw new IOException("Missing file " + agentProperties);
        }
    }

    private Set<String> getPrefixedProperty(Properties props, String featurePrefix) {
        return getPrefixedProperty(props, featurePrefix, null);
    }

    private Set<String> getPrefixedProperty(Properties props, String prefix, Map<String, String> idMap) {

        final Set<String> values = new HashSet<String>();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {

            final String key = entry.getKey().toString();
            if (key.startsWith(prefix)) {
                final String value = entry.getValue().toString();
                values.add(value);

                if (idMap != null) {
                    idMap.put(key.substring(prefix.length()), value);
                }
            }
        }

        return values;
    }
}
