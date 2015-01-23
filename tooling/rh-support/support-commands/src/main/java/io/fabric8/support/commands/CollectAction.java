/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.support.commands;

import io.fabric8.support.api.SupportService;
import org.apache.felix.gogo.commands.Command;

import java.io.File;

@Command(scope = Collect.SCOPE_VALUE, name = Collect.FUNCTION_VALUE, description = Collect.DESCRIPTION)
public class CollectAction extends AbstractSupportAction {

    protected CollectAction(SupportService service) {
        super(service);
    }

    @Override
    protected void doExecute(SupportService service) {
        System.out.println("Collecting support information...");
        File result = service.collect();
        System.out.printf("...done!%n%nSupport information available in %s%n", result.getAbsolutePath());
    }


}
