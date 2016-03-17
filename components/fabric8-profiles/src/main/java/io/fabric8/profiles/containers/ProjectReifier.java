package io.fabric8.profiles.containers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import io.fabric8.profiles.Profiles;

/**
 * Reifies projects using container config and profiles.
 */
public interface ProjectReifier {

    /**
     * Reify container.
     * @param target        output directory.
     * @param config        container config, drives Reifier behavior.
     * @param profiles      profile repository.
     * @param profileNames  container profiles.
     * @throws IOException  on error.
     */
    void reify(Path target, Properties config, Profiles profiles, String... profileNames) throws IOException;
}
