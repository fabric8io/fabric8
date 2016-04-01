/**
 * Copyright 2005-2015 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.openshift.assertions;

import io.fabric8.kubernetes.assertions.HasMetadatasAssert;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.List;

/**
 * Adds some extra assertion operations
 */
public class BuildsAssert extends HasMetadatasAssert<Build, BuildsAssert> {
    private final OpenShiftClient client;

    public BuildsAssert(List<Build> actual, OpenShiftClient client) {
        super(actual);
        this.client = client;
    }

    @Override
    protected BuildsAssert createListAssert(List<Build> list) {
        return new BuildsAssert(list, client);
    }
}
