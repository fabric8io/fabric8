/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.git.internal;

/**
 * Provides git context information so that the git operation
 * can automatically
 */
public class GitContext {

    private boolean requirePush;
    private String pushBranch;
    private boolean requireCommit;
    private StringBuilder commitMessage = new StringBuilder();

    /**
     * Indicates a  push will be required after the operation is completed.
     */
    public GitContext requirePush() {
        setRequirePush(true);
        return this;
    }

    public boolean isRequirePush() {
        return requirePush;
    }

    public void setRequirePush(boolean requirePush) {
        this.requirePush = requirePush;
    }

    /**
     * Indicates a commit is required after this operation completes
     */
    public GitContext requireCommit() {
        setRequireCommit(true);
        return this;
    }

    public boolean isRequireCommit() {
        return requireCommit;
    }

    public void setRequireCommit(boolean requireCommit) {
        this.requireCommit = requireCommit;
    }

    public String getPushBranch() {
        return pushBranch;
    }

    public void setPushBranch(String pushBranch) {
        this.pushBranch = pushBranch;
    }

    /**
     * Marks a commit as required and appends the message.
     */
    public void commit(String message) {
        setRequireCommit(true);
        if (commitMessage.length() > 0) {
            commitMessage.append("\n");
        }
        commitMessage.append(message);
    }

    /**
     * Provides a hook for a commit message if an operation wishes to add something
     */
    public StringBuilder getCommitMessage() {
        return commitMessage;
    }
}
