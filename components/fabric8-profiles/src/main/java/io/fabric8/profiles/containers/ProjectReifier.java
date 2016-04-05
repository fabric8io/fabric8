package io.fabric8.profiles.containers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.Log4JLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reifies projects using container config and profiles.
 */
public abstract class ProjectReifier {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final Properties defaultProperties;
    protected final VelocityEngine engine;

    /**
     * Configures reifier with default properties and a velocity engine.
     * @param properties default property values.
     */
    public ProjectReifier(Properties properties) {
        this.defaultProperties = new Properties();
        if (properties != null) {
            this.defaultProperties.putAll(properties);
        }

        // initialize velocity to load resources from class loader and use Log4J
        Properties velocityProperties = new Properties();
        velocityProperties.setProperty(RuntimeConstants.RESOURCE_LOADER, "cloader");
        velocityProperties.setProperty("cloader.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityProperties.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, Log4JLogChute.class.getName());
        velocityProperties.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM + ".log4j.logger", log.getName());
        engine = new VelocityEngine(velocityProperties);
        engine.init();
    }

    /**
     * Reify container.
     * @param target        output directory.
     * @param config        container config, drives Reifier behavior.
     * @param profilesDir   profile directory with materialized profiles.
     * @throws IOException  on error.
     */
    public abstract void reify(Path target, Properties config, Path profilesDir) throws IOException;
}
