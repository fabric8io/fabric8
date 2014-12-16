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
package io.fabric8.forge.camel.commands.jolokia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.camel.commands.jolokia.JolokiaCamelController;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;
import org.jboss.forge.addon.ui.input.UIInput;

public class RouteCompleter implements UICompleter<String> {

    private final JolokiaCamelController controller;
    private final UIInput<String> name;

    public RouteCompleter(JolokiaCamelController controller, UIInput<String> name) {
        this.controller = controller;
        this.name = name;
    }

    @Override
    public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
        List<String> answer = new ArrayList<>();
        try {
            // limit routes to the context if we have already selected a value
            List<Map<String, String>> contexts = controller.getRoutes(name.getValue());
            for (Map<String, String> row : contexts) {
                final String name = row.get("routeId");
                if (value == null || name.startsWith(value)) {
                    answer.add(name);
                }
            }
        } catch (Exception e) {
            // ignore
        }

        Collections.sort(answer);
        return answer;
    }


}
