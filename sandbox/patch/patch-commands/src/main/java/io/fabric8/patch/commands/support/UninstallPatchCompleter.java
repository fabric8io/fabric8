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
package io.fabric8.patch.commands.support;

import java.util.List;

import io.fabric8.patch.Patch;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

@Component(immediate = true)
@Service({UninstallPatchCompleter.class, Completer.class})
public class UninstallPatchCompleter implements Completer {

    @Reference
    private io.fabric8.patch.Service service;
    private boolean installed;
    private boolean uninstalled;

    public UninstallPatchCompleter() {
        this.installed = false;
        this.uninstalled = true;
    }

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        for (Patch patch : service.getPatches()) {
            if (isInstalled() && patch.isInstalled()
                    || isUninstalled() && !patch.isInstalled()) {
                delegate.getStrings().add(patch.getId());
            }
        }
        return delegate.complete(buffer, cursor, candidates);
    }

    public boolean isInstalled() {
        return installed;
    }

    public boolean isUninstalled() {
        return uninstalled;
    }

    public io.fabric8.patch.Service getService() {
        return service;
    }

    public void setService(io.fabric8.patch.Service service) {
        this.service = service;
    }
}
