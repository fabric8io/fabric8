/**
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
package org.fusesource.patch.commands.support;

import java.util.List;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.fusesource.patch.Patch;
import org.fusesource.patch.Service;

public class PatchCompleter implements Completer {
    
    private Service service;
    private boolean installed;
    private boolean uninstalled;

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

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public boolean isUninstalled() {
        return uninstalled;
    }

    public void setUninstalled(boolean uninstalled) {
        this.uninstalled = uninstalled;
    }
}
