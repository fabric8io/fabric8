package org.fusesource.fabric.maven.impl;

public class InvalidMavenArtifactRequest extends Exception {
    public InvalidMavenArtifactRequest() {
    }

    public InvalidMavenArtifactRequest(String s) {
        super(s);
    }

    public InvalidMavenArtifactRequest(String s, Throwable throwable) {
        super(s, throwable);
    }

    public InvalidMavenArtifactRequest(Throwable throwable) {
        super(throwable);
    }
}
