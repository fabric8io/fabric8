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

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.ProcessManager;
import io.fabric8.process.manager.commands.support.ProcessCommandSupport;
import io.fabric8.utils.TablePrinter;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(name = "ps", scope = "process", description = "Lists the currently installed managed processes.")
public class ProcessListAction extends ProcessCommandSupport {

    public ProcessListAction(ProcessManager processManager) {
        super(processManager);
    }

    @Option(name="-v", aliases={"--verbose"}, required = false, description = "Verbose option")
    protected boolean verbose;

    @Override
    protected Object doExecute() throws Exception {
        List<Installation> installations = getProcessManager().listInstallations();

        printInstallations(installations, System.out);
        return null;
    }

    protected void printInstallations(List<Installation> installations, PrintStream out) {
        TablePrinter printer = new TablePrinter();
        if (verbose) {
            printer.columns("id", "pid", "connected", "type", "directory");
        } else {
            printer.columns("id", "pid", "connected", "type");
        }

        for (Installation installation : installations) {

            String id = installation.getId();
            String pid = "";
            String connected = "no";
            String path = installation.getInstallDir() != null ? installation.getInstallDir().getPath() : "";

            String type = installation.getName();
            if (!verbose) {
                if (type.startsWith("java ")) {
                    // skip middle package name as that is too verbose
                    int idx = type.lastIndexOf('.');
                    if (idx > 0) {
                        type = "java " + type.substring(idx + 1);
                    }
                }
            }
            try {
                Long aid = installation.getActivePid();
                if (aid != null) {
                    pid = "" + aid;
                    connected = "yes";
                }
            } catch (IOException e) {
                // ignore
            }
            if (verbose) {
                printer.row(id, pid, connected, type, path);
            } else {
                printer.row(id, pid, connected, type);
            }
        }

        printer.print(out);
    }
}
