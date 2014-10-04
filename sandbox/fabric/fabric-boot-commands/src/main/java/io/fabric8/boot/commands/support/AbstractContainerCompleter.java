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
package io.fabric8.boot.commands.support;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;

import java.util.List;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

public abstract class AbstractContainerCompleter extends AbstractCompleterComponent implements Completer {

    @Override
    public String getParameter() {
        return "--container";
    }

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
            StringsCompleter delegate = new StringsCompleter();
            for (Container container : getFabricService().getContainers()) {
                if (apply(container)) {
                    delegate.getStrings().add(container.getId());
                }
            }
            return delegate.complete(buffer, cursor, candidates);
    }

    protected abstract FabricService getFabricService();
    protected abstract boolean apply(Container container);
}
