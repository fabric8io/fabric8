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

public class RouteCompleter implements UICompleter<String> {

    private final JolokiaCamelController controller;

    public RouteCompleter(JolokiaCamelController controller) {
        this.controller = controller;
    }

    @Override
    public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
        List<String> answer = new ArrayList<>();
        try {
            List<Map<String, String>> contexts = controller.getRoutes(null);
            for (Map<String, String> row : contexts) {
                final String name = row.get("routeId");
                answer.add(name);
            }
        } catch (Exception e) {
            // ignore
        }

        Collections.sort(answer);
        return answer;
    }


}
