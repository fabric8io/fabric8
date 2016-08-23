/**
 * Copyright 2005-2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.karaf.common.properties;

import java.util.concurrent.CopyOnWriteArrayList;

import io.fabric8.karaf.common.Support;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

@Component(
    immediate = true,
    policy = ConfigurationPolicy.IGNORE,
    createPid = false
)
@Reference(
    name = "function",
    cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
    policy = ReferencePolicy.DYNAMIC,
    referenceInterface = PropertiesFunction.class
)
@Service(Fabric8PlaceholderResolver.class)
public class Fabric8PlaceholderResolver {
    private final CopyOnWriteArrayList<PropertiesFunction> functions;

    public Fabric8PlaceholderResolver() {
        functions = new CopyOnWriteArrayList<>();
    }

    public String resolve(String value) {
        String[] resolvers = Support.before(value, ":").split("\\+");
        String remainder = Support.after(value, ":");

        for (String resolver : resolvers) {
            PropertiesFunction function = findFunction(resolver);

            if (function == null) {
                value = null;
                break;
            }

            value = function.apply(remainder);
            if (value != null) {
                remainder = value;
            } else {
                break;
            }
        }

        return value;
    }

    // ****************************
    // Binding
    // ****************************

    protected void bindFunction(PropertiesFunction function) {
        functions.addIfAbsent(function);
    }

    protected void unbindFunction(PropertiesFunction function) {
        functions.remove(function);
    }

    // ****************************
    // Helpers
    // ****************************

    private PropertiesFunction findFunction(String name) {
        for (PropertiesFunction fun : functions) {
            if (name.equals(fun.getName())) {
                return fun;
            }
        }
        return null;
    }
}
