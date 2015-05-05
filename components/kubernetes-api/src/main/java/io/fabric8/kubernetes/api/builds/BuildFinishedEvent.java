/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api.builds;

import io.fabric8.openshift.api.model.Build;

/**
 */
public class BuildFinishedEvent {
    private final String uid;
    private final Build build;
    private final boolean loading;
    private final String buildLink;

    public BuildFinishedEvent(String uid, Build build, boolean loading, String buildLink) {
        this.uid = uid;
        this.build = build;
        this.loading = loading;
        this.buildLink = buildLink;
    }

    public String getUid() {
        return uid;
    }

    public Build getBuild() {
        return build;
    }

    public String getBuildLink() {
        return buildLink;
    }

    public boolean isLoading() {
        return loading;
    }

    public String getStatus() {
        return build.getStatus().getPhase();
    }

    public String getConfigName() {
        return Builds.getBuildConfigName(build);
    }

    public String getNamespace() {
        return Builds.getNamespace(build);
    }
}
