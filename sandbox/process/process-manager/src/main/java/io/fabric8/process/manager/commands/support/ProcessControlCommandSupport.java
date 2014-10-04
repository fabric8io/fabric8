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
package io.fabric8.process.manager.commands.support;

import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.ProcessManager;
import org.apache.felix.gogo.commands.Argument;

import java.util.Map;

/**
 */
public abstract class ProcessControlCommandSupport extends ProcessCommandSupport {

    @Argument(index = 0, required = true, multiValued = true, name = "id", description = "The id of the managed processes to control")
    protected String[] ids;

    protected ProcessControlCommandSupport(ProcessManager processManager) {
        super(processManager);
    }

    @Override
    protected Object doExecute() throws Exception {
        Map<String, Installation> map = getProcessManager().listInstallationMap();
        for (String id : ids) {
            Installation installation = map.get(id);
            if (installation == null) {
                System.out.println("No such process number: " + id);
            } else {
                doControlCommand(installation);
            }
        }
        return null;
    }

    protected abstract void doControlCommand(Installation installation) throws Exception;
}
