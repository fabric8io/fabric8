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
package io.fabric8.patch.commands;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.patch.Service;

@Command(scope = "patch", name = "list", description = "Display known patches")
public class ListAction extends PatchActionSupport {

    @Option(name = "--bundles", description = "Display the list of bundles for each patch")
    boolean bundles;

    ListAction(Service service) {
        super(service);
    }

    @Override
    protected void doExecute(Service service) throws Exception {
        display(service.getPatches(), bundles);
    }

}
