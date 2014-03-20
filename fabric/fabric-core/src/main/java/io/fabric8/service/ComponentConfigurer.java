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
package io.fabric8.service;

import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.api.scr.support.ConfigInjection;
import io.fabric8.utils.Strings;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(immediate = true)
@Service(Configurer.class)
public class ComponentConfigurer extends AbstractComponent implements Configurer {

    private static final String ENV_VAR_PREFIX = "FABRIC8_";
    private static final String REPLACE_PATTERN = "-|\\.";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9\\.\\-]+)}");
    private static final String BOX_FORMAT = "\\$\\{%s\\}";

    @Reference(referenceInterface = RuntimeProperties.class, bind = "bindRuntimeProperties", unbind = "unbindRuntimeProperties")
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }


    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }

    @Override
    public <T> void configure(Map<String, ?> configuration, T target) throws Exception {
        assertValid();
        final RuntimeProperties properties = runtimeProperties.get();
        Map<String, Object> result = new HashMap<String, Object>();


        for (Map.Entry<String, ?> entry : configuration.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value.getClass().isArray()) {
                //do nothing
            } else if (value instanceof String) {
                String substitutedValue = substitute((String) value, properties);
                //We don't want to inject blanks. If substitution fails, do not inject.
                if (Strings.isNotBlank(substitutedValue)) {
                    result.put(key, substitutedValue);
                }
            }
        }
        ConfigInjection.applyConfiguration(result, target);
    }


    static String substitute(String key, RuntimeProperties properties) {
        String result = new String(key);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(key);
        while (matcher.find()) {
            String name = matcher.group(1);
            String toReplace = String.format(BOX_FORMAT, name);
            String replacement = properties.getProperty(name);
            replacement = Strings.isNotBlank(replacement) ? replacement : "";
            result = result.replaceAll(toReplace, Matcher.quoteReplacement(replacement));
        }
        return result;
    }
}
