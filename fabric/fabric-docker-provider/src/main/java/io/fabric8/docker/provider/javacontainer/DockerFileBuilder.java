package io.fabric8.docker.provider.javacontainer;

import java.io.File;
import java.io.IOException;

import io.fabric8.common.util.Files;

/**
 *
 */
public class DockerFileBuilder {

    public StringBuilder sb = new StringBuilder();

    private DockerFileBuilder(String from) {
        fromInternal(from);
    }

    public static DockerFileBuilder from(String image) {
        return new DockerFileBuilder(image);
    }

    private DockerFileBuilder fromInternal(String image) {
        sb.append("FROM ").append(image).append("\n\n");
        return this;
    }

    public DockerFileBuilder maintainer(String name) {
        sb.append("MAINTAINER ").append(name).append("\n");
        return this;
    }

    public DockerFileBuilder run(String... command) {
        appendMultiArgInstruction("RUN", command);
        return this;
    }

    private void appendMultiArgInstruction(String instruction, String... command) {
        if (command.length == 1) {
            sb.append(instruction).append(" ").append(command[0]);
        } else {
            sb.append(instruction).append(" ");
            sb.append("[");
            for (String arg : command) {
                sb.append("\"").append(escapeParentheses(arg)).append("\"").append(", ");
            }
            sb.setLength(sb.length()-2);    // remove last ", "
            sb.append("]");
        }
        sb.append("\n");
    }

    public DockerFileBuilder cmd(String... command) {
        appendMultiArgInstruction("CMD", command);
        return this;
    }

    public DockerFileBuilder expose(String... ports) {
        sb.append("EXPOSE");
        for (String port : ports) {
            sb.append(" ").append(port);
        }
        sb.append("\n");
        return this;
    }

    public DockerFileBuilder env(String key, String value) {
        sb.append("ENV ").append(key).append(" ").append(value).append("\n");
        return this;
    }

    public DockerFileBuilder add(String src, String dest) {
        sb.append("ADD ").append(src).append(" ").append(dest).append("\n");
        return this;
    }

    public DockerFileBuilder entrypoint(String... command) {
        appendMultiArgInstruction("ENTRYPOINT", command);
        return this;
    }

    public DockerFileBuilder volume(String path) {
        sb.append("VOLUME [").append(path).append("]\n");
        return this;
    }

    public DockerFileBuilder user(String user) {
        sb.append("USER ").append(user).append("\n");
        return this;
    }

    public DockerFileBuilder workdir(String path) {
        sb.append("WORKDIR ").append(path).append("\n");
        return this;
    }

    // TODO: this could take another DockerFileBuilder instead of a raw instruction
    public DockerFileBuilder onbuild(String instruction) {
        sb.append("ONBUILD ").append(instruction).append("\n");
        return this;
    }

    private String escapeParentheses(String str) {
        return str.replaceAll("\"", "\\\"");
    }

    public String build() {
        return toString();
    }

    public String toString() {
        return sb.toString();
    }

    public void writeTo(File file) throws IOException {
        Files.writeToFile(file, toString().getBytes());
    }
}
