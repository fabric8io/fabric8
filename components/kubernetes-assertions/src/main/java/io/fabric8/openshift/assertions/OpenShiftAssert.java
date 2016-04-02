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

import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigList;
import io.fabric8.openshift.api.model.BuildList;
import io.fabric8.openshift.client.OpenShiftClient;
import org.assertj.core.api.AbstractAssert;

import java.util.List;

import static io.fabric8.kubernetes.assertions.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * An assertion class for making assertions about the OpenShift specific resources 
 * using the <a href="http://joel-costigliola.github.io/assertj">assertj library</a>
 */
public class OpenShiftAssert extends AbstractAssert<OpenShiftAssert, OpenShiftClient> {
    private final OpenShiftClient client;

    public OpenShiftAssert(OpenShiftClient client) {
        super(client, OpenShiftAssert.class);
        this.client = client;
    }

    public BuildConfigsAssert buildConfigs() {
        BuildConfigList listObject = client.buildConfigs().list();
        assertThat(listObject).describedAs("No BuildConfigsList found!").isNotNull();
        List<BuildConfig> list = listObject.getItems();
        assertThat(list).describedAs("No BuildConfig Items found!").isNotNull();
        return new BuildConfigsAssert(list, client);
    }

    public BuildsAssert builds() {
        BuildList listObject = client.builds().list();
        assertThat(listObject).describedAs("No BuildList found!").isNotNull();
        List<Build> list = listObject.getItems();
        assertThat(list).describedAs("No Build Items found!").isNotNull();
        return new BuildsAssert(list, client);
    }

}
