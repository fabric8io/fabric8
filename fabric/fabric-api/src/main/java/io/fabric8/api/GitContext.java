/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.api;


/**
 * Provides git context information
 */
public class GitContext {

    private boolean requirePull;
    private boolean requirePush;
    private boolean requireCommit;
    private String pushBranch;
    private StringBuilder commitMessage = new StringBuilder();

    /**
     * Indicates a pull will be required before the operation is executed.
     */
    public GitContext requirePull() {
        setRequirePull(true);
        return this;
    }

    public boolean isRequirePull() {
        return requirePull;
    }

    public GitContext setRequirePull(boolean requirePull) {
        this.requirePull = requirePull;
        return this;
    }

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

    public GitContext setRequirePush(boolean requirePush) {
        this.requirePush = requirePush;
        return this;
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

    public GitContext setRequireCommit(boolean requireCommit) {
        this.requireCommit = requireCommit;
        return this;
    }

    public String getPushBranch() {
        return pushBranch;
    }

    public GitContext setPushBranch(String pushBranch) {
        this.pushBranch = pushBranch;
        return this;
    }

    /**
     * Append the commit message.
     */
    public GitContext commitMessage(String message) {
        if (commitMessage.length() > 0) {
            commitMessage.append("\n-");
        }
        commitMessage.append(message);
        return this;
    }

    /**
     * Get the commit message
     */
    public String getCommitMessage() {
        return commitMessage.toString();
    }
}
