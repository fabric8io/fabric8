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
package io.fabric8.commands;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.shell.console.CommandLoggingFilter;
import org.apache.karaf.shell.console.RegexCommandLoggingFilter;

/**
 * Used to filter out passwords from command logging.
 */
@Component(immediate = true)
@Service({CommandLoggingFilter.class})
public class FabricCommandLoggingFilter extends RegexCommandLoggingFilter {
    public FabricCommandLoggingFilter() {

        addCommandOption("--jmx-password", ContainerCreateChild.FUNCTION_VALUE, MQCreate.FUNCTION_VALUE);
        addCommandOption("--zookeeper-password", ContainerCreateChild.FUNCTION_VALUE);
        addCommandOption("--new-zookeeper-password", EnsembleAdd.FUNCTION_VALUE, EnsembleRemove.FUNCTION_VALUE);
        addCommandOption("--networks-password", MQCreate.FUNCTION_VALUE);
        addCommandOption("-p", ContainerConnect.FUNCTION_VALUE,
                ContainerEditJvmOptions.FUNCTION_VALUE,
                Encrypt.FUNCTION_VALUE,
                PatchApply.FUNCTION_VALUE);
        addCommandOption("--password",
                ContainerConnect.FUNCTION_VALUE,
                ContainerEditJvmOptions.FUNCTION_VALUE,
                Encrypt.FUNCTION_VALUE,
                PatchApply.FUNCTION_VALUE,
                ContainerDelete.FUNCTION_VALUE,
                ContainerStop.FUNCTION_VALUE,
                ContainerStart.FUNCTION_VALUE);
    }
}
