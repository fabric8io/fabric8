/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.process.manager.commands;

import com.google.common.base.Preconditions;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.fusesource.process.manager.Installation;
import org.fusesource.process.manager.ProcessManager;

import java.util.List;

/**
 */
public class ProcessNumberCompleter implements Completer {
    private ProcessManager processManager;

    public ProcessManager getProcessManager() {
        return processManager;
    }

    public void setProcessManager(ProcessManager processManager) {
        this.processManager = processManager;
    }

    public void init() {
        Preconditions.checkNotNull(processManager, "processManager property");
    }

    @Override
    public int complete(final String buffer, final int cursor, final List candidates) {
        StringsCompleter delegate = new StringsCompleter();

        List<Installation> installations = processManager.listInstallations();
        for (Installation installation : installations) {
            String id = "" + installation.getId();
            delegate.getStrings().add(id);
        }
        return delegate.complete(buffer, cursor, candidates);
    }
}
