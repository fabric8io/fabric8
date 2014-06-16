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
package io.fabric8.process.manager.commands;

import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.ProcessManager;
import io.fabric8.process.manager.commands.support.ProcessControlCommandSupport;
import org.apache.felix.gogo.commands.Command;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 */
@Command(name = "environment", scope = "process", description = "Environment variables of a managed process")
public class EnvironmentAction extends ProcessControlCommandSupport {

    static final String[] HEADERS = {"[Variable]", "[Value]"};

    static final String FORMAT = "%-45s %s";

    protected EnvironmentAction(ProcessManager processManager) {
        super(processManager);
    }

    @Override
    protected void doControlCommand(Installation installation) throws Exception {
        printEnvironment(installation, System.out);
    }

    protected void printEnvironment(Installation installation, PrintStream out) {
        out.println(String.format(FORMAT, HEADERS));
        for (String variable : installation.getEnvironment().keySet()) {
            String value = installation.getEnvironment().get(variable);
            out.println(String.format(FORMAT, variable, value));
        }
    }

}
