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

import java.util.List;

import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.ProcessManager;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

/**
 */
@Component(immediate = true)
@Service({ProcessNumberCompleter.class, Completer.class})
public class ProcessNumberCompleter implements Completer {

    @Reference
    private ProcessManager processManager;

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
