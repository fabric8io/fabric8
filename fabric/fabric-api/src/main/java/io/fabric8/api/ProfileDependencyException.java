package io.fabric8.api;

import java.util.Arrays;
import java.util.Set;

/**
 * @author Ken Finnigan
 */
public class ProfileDependencyException extends FabricException {

    public ProfileDependencyException(Set<String> profiles, String[] dependencies, String[] tags) {
        this(profiles, dependencies, tags, null);
    }

    public ProfileDependencyException(Set<String> profiles, String[] dependencies, String[] tags, Throwable cause) {
        super("Unable to create container for " + Arrays.toString(profiles.toArray()) + ", missing dependant container matching profiles: "
                + Arrays.toString(dependencies) + "; or tags: " + Arrays.toString(tags), cause);
    }
}
