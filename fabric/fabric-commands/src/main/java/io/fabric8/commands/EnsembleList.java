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
package io.fabric8.commands;

import java.io.PrintStream;
import java.util.List;

import org.apache.felix.gogo.commands.Command;
import io.fabric8.boot.commands.support.EnsembleCommandSupport;

@Command(name = "ensemble-list", scope = "fabric", description = "List the containers in the current fabric ensemble (ZooKeeper ensemble)", detailedDescription = "classpath:ensembleList.txt")
public class EnsembleList extends EnsembleCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        PrintStream out = System.out;
        List<String> containers = service.getEnsembleContainers();
        if (containers != null) {
            out.println("[id]");
            for (String container : containers) {
                out.println(container);
            }
        }
        return null;
    }

}
