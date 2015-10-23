package io.fabric8.forge.rest.git.dto;

import java.util.Date;

/**
 * Represents information about a commit log or history
 */
public class StatusDTO extends GitDTOSupport {
    private final String operation;
    private final String file;

    public StatusDTO(String file, String operation) {
        this.file = file;
        this.operation = operation;
    }

    @Override
    public String toString() {
        return "StatusDTO{" +
                "file='" + file + '\'' +
                ", operation='" + operation + '\'' +
                '}';
    }

    public String getFile() {
        return file;
    }

    public String getOperation() {
        return operation;
    }
}
