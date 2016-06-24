/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.kubernetes.api.builds;

/**
 * A strategy for processing completed builds
 */
public interface BuildListener {

    /**
     * The build that has completed (successfully or not) and the flag indicating whether or not
     * this is the first time the watcher is being started up (so you should check if you've already
     * received and processed this event before).
     */
    void onBuildFinished(BuildFinishedEvent event);
}
