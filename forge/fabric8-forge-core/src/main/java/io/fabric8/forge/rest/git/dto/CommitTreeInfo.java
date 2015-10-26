/**
 * Copyright (C) 2013 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.rest.git.dto;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.FileMode;

/**
 * Represent part of a commit
 */
public class CommitTreeInfo {
    public DiffEntry.ChangeType changeType;
    private int mode;
    private String path;
    private String name;
    private String id;
    private long size;
    private String commitId;
    private boolean isParentPath;

    public CommitTreeInfo() {
    }

    public CommitTreeInfo(String path, String name, long size, int mode, String id, String commitId, DiffEntry.ChangeType changeType) {
        this.path = path;
        this.name = name;
        this.size = size;
        this.mode = mode;
        this.id = id;
        this.commitId = commitId;
        this.changeType = changeType;
    }

    @Override
    public String toString() {
        return "CommitTreeInfo{" +
                "mode=" + mode +
                ", path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", size=" + size +
                ", commitId='" + commitId + '\'' +
                ", isParentPath=" + isParentPath +
                ", changeType=" + changeType +
                '}';
    }

    public boolean isSymlink() {
        return FileMode.SYMLINK.equals(mode);
    }

    public boolean isSubmodule() {
        return FileMode.GITLINK.equals(mode);
    }

    public boolean isTree() {
        return FileMode.TREE.equals(mode);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public boolean isParentPath() {
        return isParentPath;
    }

    public void setParentPath(boolean isParentPath) {
        this.isParentPath = isParentPath;
    }

    public DiffEntry.ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(DiffEntry.ChangeType changeType) {
        this.changeType = changeType;
    }
}
