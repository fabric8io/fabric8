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
package io.fabric8.agent.download;

import java.io.File;

import io.fabric8.api.Profile;

/**
 * Listener to receive events when using {@link io.fabric8.agent.download.ProfileDownloader}.
 */
public interface ProfileDownloaderListener {

    void beforeDownloadProfiles(Profile[] profiles);

    void afterDownloadProfiles(Profile[] profiles);

    void beforeDownloadProfile(Profile profile);

    void afterDownloadProfile(Profile profile);

    void onCopyDone(Profile profile, File destination);

    void onError(Profile profile, Exception cause);

}
