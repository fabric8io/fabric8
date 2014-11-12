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
package io.fabric8.git;

import io.fabric8.api.GitContext;

import java.util.List;
import java.util.Set;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;

public interface PullPushPolicy {

    interface PullPolicyResult {
        
        boolean localUpdateRequired();
        
        boolean remoteUpdateRequired();
        
        Set<String> getVersions();
        
        Exception getLastException();
    }

    interface PushPolicyResult {
    
        List<PushResult> getPushResults();
        
        List<RemoteRefUpdate> getAcceptedUpdates();
        
        List<RemoteRefUpdate> getRejectedUpdates();
        
        Exception getLastException();
    }
    
    /**
     * Pull the version/profile state from the remote repository
     */
    PullPolicyResult doPull(GitContext context, CredentialsProvider credentialsProvider, boolean allowVersionDelete);

    /**
     * Push the version/profile state to the remote repository
     */
    PushPolicyResult doPush(GitContext context, CredentialsProvider credentialsProvider);

}
