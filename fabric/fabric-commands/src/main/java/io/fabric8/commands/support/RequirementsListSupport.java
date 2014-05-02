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
package io.fabric8.commands.support;

import java.io.PrintStream;

import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import org.apache.karaf.shell.console.AbstractAction;

public abstract class RequirementsListSupport extends AbstractAction {

    private final FabricService fabricService;

    public RequirementsListSupport(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        PrintStream out = System.out;
        FabricRequirements requirements = getFabricService().getRequirements();
        printRequirements(out, requirements);
        return null;
    }

    protected abstract void printRequirements(PrintStream out, FabricRequirements requirements);


}
