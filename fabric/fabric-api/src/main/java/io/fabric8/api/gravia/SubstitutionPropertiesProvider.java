/*
 * #%L
 * Gravia :: Runtime :: API
 * %%
 * Copyright (C) 2013 - 2014 JBoss by Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.fabric8.api.gravia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link PropertiesProvider} that is applying placeholder substitution based on the property values of an external {@link PropertiesProvider}.
 */
public class SubstitutionPropertiesProvider extends CompositePropertiesProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubstitutionPropertiesProvider.class);

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9\\.\\-]+)}");
    private static final String BOX_FORMAT = "\\$\\{%s\\}";
    private static final String UNESCAPED_BOX_FORMAT = "${%s}";

    public SubstitutionPropertiesProvider(PropertiesProvider... delegates) {
        super(delegates);
    }

    @Override
    public Object getProperty(String key) {
        return getProperty(key, null);
    }

    @Override
    public Object getProperty(String key, Object defaultValue) {
        for (PropertiesProvider provider : getDelegates()) {
            try {
                Object rawValue = provider.getProperty(key);
                if (rawValue != null && !isCyclicReference(key, rawValue)) {
                    return substitute(String.valueOf(rawValue), new HashSet<String>());
                }
            } catch (Exception e) {
                LOGGER.debug("Skipping properties provider:{}, due to:{}", provider, e.getMessage());
            }
        }
        return defaultValue;
    }


    /**
     * Substitutes placeholders in the specified string, taking loops into consideration.
     * It uses the delegating property providers to resolve the placeholders.
     * @param str          The string that contains one or more placeholders.
     * @param visited      The placeholders that are considered visited (use for loop detection).
     * @return             The substituted string, if all placeholders have been successfully resolved.
     *                     Returns null if str contains a single unresolved placeholder (to allow falling back to
     *                     the default value).
     *                     In case of partial success (some placeholder resolved) it returns the result using a empty
     *                     string for all the unresolved ones.
     */
    private String substitute(String str, Set<String> visited) {
        String result = str;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(str);
        CopyOnWriteArraySet<String> copyOfVisited = new CopyOnWriteArraySet<>(visited);
        while (matcher.find()) {
            String name = matcher.group(1);
            String replacement = null;
            String toReplace = String.format(BOX_FORMAT, name);
            for (PropertiesProvider provider : getDelegates()) {
                if (provider.getProperty(name) != null && !visited.contains(name)) {
                    Object rawValue = provider.getProperty(name);
                    if (isCyclicReference(name, rawValue)) {
                        continue;
                    }
                    replacement = String.valueOf(rawValue);
                    if (PLACEHOLDER_PATTERN.matcher(replacement).matches()) {
                        copyOfVisited.add(name);
                        replacement = substitute(replacement, copyOfVisited);
                    }
                }
            }
            if (replacement != null) {
                result = result.replaceAll(toReplace, Matcher.quoteReplacement(replacement));
            } else if (!str.equals(toReplace.replace("\\",""))) {
                result = result.replaceAll(toReplace, "");
            } else {
                result = null;
            }
        }
        return result;
    }

    /**
     * Checks if the value is a placeholder reference to the key.
     * @param key       The key.
     * @param value     The value.
     * @return          True if a cycle is detected or false.
     */
    private static boolean isCyclicReference(String key, Object value) {
        if (!(value instanceof String)) {
            return false;
        } else return String.format(UNESCAPED_BOX_FORMAT, key).equals(value);
    }
}
