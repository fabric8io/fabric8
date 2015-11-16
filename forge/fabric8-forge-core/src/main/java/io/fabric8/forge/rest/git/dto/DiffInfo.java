/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

/**
 */
public class DiffInfo extends GitDTOSupport {
    private final DiffEntry.ChangeType changeType;
    private final String newPath;
    private final int newMode;
    private final String oldPath;
    private final int oldMode;
    private final String diff;

    public DiffInfo(DiffEntry.ChangeType changeType, String newPath, int newMode, String oldPath, int oldMode, String diff) {
        this.changeType = changeType;
        this.newPath = newPath;
        this.newMode = newMode;
        this.oldPath = oldPath;
        this.oldMode = oldMode;
        this.diff = diff;
    }

    @Override
    public String toString() {
        return "DiffInfo{" +
                "changeType=" + changeType +
                ", newPath='" + newPath + '\'' +
                ", oldPath='" + oldPath + '\'' +
                '}';
    }

    public DiffEntry.ChangeType getChangeType() {
        return changeType;
    }

    public String getDiff() {
        return diff;
    }

    public int getNewMode() {
        return newMode;
    }

    public String getNewPath() {
        return newPath;
    }

    public int getOldMode() {
        return oldMode;
    }

    public String getOldPath() {
        return oldPath;
    }
}
