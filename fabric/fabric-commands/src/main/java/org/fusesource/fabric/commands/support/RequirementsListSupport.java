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
package org.fusesource.fabric.commands.support;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.boot.commands.support.FabricCommand;
import org.fusesource.fabric.service.FabricServiceImpl;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

/**
 */
public abstract class RequirementsListSupport extends FabricCommand {
    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        PrintStream out = System.out;
        FabricRequirements requirements = fabricService.getRequirements();
        if (requirements == null) {
            out.println("No requirements are defined for this fabric. Please create a requirements JSON file in " + FabricServiceImpl.requirementsJsonPath);
        } else {
            printRequirements(out, requirements);
        }
        return null;
    }

    protected abstract void printRequirements(PrintStream out, FabricRequirements requirements);


    protected String percentText(double value) {
        return NumberFormat.getPercentInstance().format(value);
    }

}
