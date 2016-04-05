package io.fabric8.profiles.maven;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import io.fabric8.profiles.containers.Containers;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Git aware containers generator
 */
@Mojo(name = "git-generate", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresProject = true, defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GitContainersGenerator extends ContainersGenerator {

    @Override
    protected void generateContainers(Containers containers, Path target, List<Path> names) throws IOException {
        // TODO determine which containers have changed
    }
}
