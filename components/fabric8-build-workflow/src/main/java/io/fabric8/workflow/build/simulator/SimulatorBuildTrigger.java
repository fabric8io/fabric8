/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.workflow.build.simulator;

import io.fabric8.workflow.build.trigger.BuildTrigger;

/**
 * Triggers a build in the simulator
 */
public class SimulatorBuildTrigger implements BuildTrigger {

    @Override
    public String trigger(String namespace, String buildName) {
        return BuildSimulator.getSingleton().triggerBuild(namespace, buildName);
    }
}
