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
package io.fabric8.agent.service;

public interface Constants {

    String ROOT_REGION = "root";

    String UPDATE_SNAPSHOTS_NONE = "none";
    String UPDATE_SNAPSHOTS_CRC = "crc";
    String UPDATE_SNAPSHOTS_ALWAYS = "always";

    String DEFAULT_UPDATE_SNAPSHOTS = UPDATE_SNAPSHOTS_CRC;
    String DEFAULT_FEATURE_RESOLUTION_RANGE = "${range;[====,====]}";
    String DEFAULT_BUNDLE_UPDATE_RANGE = "${range;[==,=+)}";

    String UPDATEABLE_URIS = "mvn:.*-SNAPSHOT((\\.\\w{3})?|\\$.*|\\?.*|\\#.*|\\&.*)|(?!mvn:).*";

    enum Option {
        NoFailOnFeatureNotFound,
        NoAutoRefreshManagedBundles,
        NoAutoRefreshUnmanagedBundles,
        NoAutoRefreshBundles,
        NoAutoStartBundles,
        NoAutoManageBundles,
        Simulate,
        Verbose,
        Silent
    }

    enum RequestedState {
        Installed,
        Resolved,
        Started
    }


}
