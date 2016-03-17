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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.fabric8.profiles.Profiles;
import io.fabric8.profiles.containers.ProjectReifier;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.Log4JLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.profiles.ProfilesHelpers.readPropertiesFile;

/**
 * Reify Karaf container from Profiles
 */
public class KarafProjectReifier implements ProjectReifier {

    private static final Logger LOG = LoggerFactory.getLogger(KarafProjectReifier.class);
    private static final String KARAF_POM_VM = "/containers/karaf/pom.vm";

    private static final String REPOSITORY_PREFIX = "repository.";
    private static final String FEATURE_PREFIX = "feature.";
    private static final String BUNDLE_PREFIX = "bundle.";
    private static final String OVERRIDE_PREFIX = "override.";
    private static final String CONFIG_PREFIX = "config.";
    private static final String SYSTEM_PREFIX = "system.";
    private static final String LIB_PREFIX = "lib.";

    private static final String AGENT_PROPERTIES = "io.fabric8.agent.properties";

    private final Properties defaultProperties;
    private final VelocityEngine engine;

    public KarafProjectReifier(Properties properties) {
        this.defaultProperties = new Properties();
        if (properties != null) {
            this.defaultProperties.putAll(properties);
        }

        // initialize velocity to load resources from class loader and use Log4J
        Properties velocityProperties = new Properties();
        velocityProperties.setProperty(RuntimeConstants.RESOURCE_LOADER, "cloader");
        velocityProperties.setProperty("cloader.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityProperties.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, Log4JLogChute.class.getName());
        velocityProperties.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM + ".log4j.logger", LOG.getName());
        engine = new VelocityEngine(velocityProperties);
        engine.init();
    }

    @Override
    public void reify(Path target, Properties config, Profiles profiles, String... profileNames) throws IOException {
        // temp dir for materialized profile
        final Path profilesDir = Files.createTempDirectory(target, "profiles");
        profilesDir.toFile().deleteOnExit();

        // materialize profile
        // remove ensemble profiles fabric-ensemble-*
        profileNames = Arrays.stream(profileNames).filter(new Predicate<String>() {
            @Override
            public boolean test(String p) {
                return !p.matches("fabric\\-ensemble\\-.*");
            }
        }).collect(Collectors.toList()).toArray(new String[0]);
        profiles.materialize(profilesDir, profileNames);

        // reify maven project using template
        final Properties containerProperties = new Properties(defaultProperties);
        containerProperties.putAll(config);
        reifyProject(target, profilesDir, containerProperties);
    }

    private void reifyProject(Path target, final Path profilesDir, Properties properties) throws IOException {
        final File pojoFile = new File(target.toFile(), "pom.xml");
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(pojoFile));

            VelocityContext context = new VelocityContext();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                context.put(entry.getKey().toString(), entry.getValue());
            }

            // read profile properties
            loadProperties(context, profilesDir);

            LOG.debug("Writing %s...", pojoFile);
            Template pojoTemplate = engine.getTemplate(KARAF_POM_VM);
            pojoTemplate.merge(context, writer);

            // close pojoFile
            writer.close();

            // add other resource files under src/main/resources/assembly
            final Path assemblyPath = target.resolve("src/main/resources/assembly/etc");
            LOG.debug("Writing resources to %s...", assemblyPath);
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
                    if (!AGENT_PROPERTIES.equals(file.getFileName().toString())) {
                        Files.copy(file, assemblyPath.resolve(profilesDir.relativize(file)));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            LOG.debug("Done!");

        } finally {
            if (writer != null) {
                writer.close();
            }
        }
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
