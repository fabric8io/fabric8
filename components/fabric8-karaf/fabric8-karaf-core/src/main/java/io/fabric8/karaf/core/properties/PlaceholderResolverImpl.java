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
package io.fabric8.karaf.core.properties;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.fabric8.karaf.core.Support;
import io.fabric8.karaf.core.properties.function.PropertiesFunction;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

import static io.fabric8.kubernetes.client.utils.Utils.getSystemPropertyOrEnvVar;

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
@Service(PlaceholderResolver.class)
public class PlaceholderResolverImpl implements PlaceholderResolver {
    public static final String PLACEHOLDER_PREFIX = "fabric8.placeholder.prefix";
    public static final String PLACEHOLDER_SUFFIX = "fabric8.placeholder.suffix";
    public static final String DEFAULT_PLACEHOLDER_PREFIX =  "$[";
    public static final String DEFAULT_PLACEHOLDER_SUFFIX =  "]";

    private final CopyOnWriteArrayList<PropertiesFunction> functions;
    private final StrSubstitutor substitutor;

    public PlaceholderResolverImpl() {
        this.functions = new CopyOnWriteArrayList<>();
        this.substitutor = Support.createStrSubstitutor(
            getSystemPropertyOrEnvVar(PLACEHOLDER_PREFIX, DEFAULT_PLACEHOLDER_PREFIX),
            getSystemPropertyOrEnvVar(PLACEHOLDER_SUFFIX, DEFAULT_PLACEHOLDER_SUFFIX),
            new StrLookup<String>() {
                @Override
                public String lookup(String value) {
                    return resolve(value);
                }
        });
    }

    @Override
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

    @Override
    public String replace(String value) {
        return substitutor.replace(value);
    }

    @Override
    public boolean replaceIn(StringBuilder value) {
        return substitutor.replaceIn(value);
    }

    @Override
    public boolean replaceAll(Dictionary<String, Object> dictionary) {
        int replacedCount = 0;

        Enumeration<String> keys = dictionary.keys();
        while(keys.hasMoreElements()) {
            final String key = keys.nextElement();
            final Object val = dictionary.get(key);

            if (val instanceof String) {
                StringBuilder sb = Support.acquireStringBuilder((String)val);
                if (substitutor.replaceIn(sb)) {
                    replacedCount++;
                    dictionary.put(key, sb.toString());
                }
            }
        }

        return replacedCount > 0;
    }

    @Override
    public boolean replaceAll(Map<String, Object> dictionary) {
        int replacedCount = 0;
        for(String key : dictionary.keySet()) {
            final Object val = dictionary.get(key);

            if (val instanceof String) {
                StringBuilder sb = Support.acquireStringBuilder((String)val);
                if (substitutor.replaceIn(sb)) {
                    replacedCount++;
                    dictionary.put(key, sb.toString());
                }
            }
        }

        return replacedCount > 0;
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
