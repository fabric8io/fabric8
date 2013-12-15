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
package io.fabric8.internal;

import io.fabric8.api.DynamicReference;
import io.fabric8.api.FabricException;
import io.fabric8.api.PlaceholderResolver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling {@link PlaceholderResolver} tasks.
 */
public final class PlaceholderResolverHelpers {

    public static final Pattern SCHEME_PATTERN = Pattern.compile("\\$\\{([^ :\\}]+):([^\\}]*\\})");

    private PlaceholderResolverHelpers() {
        //Utility Class
    }

    /**
     * Extracts all placeholder resolver schemes that are referred from the value.
     * @param value
     * @return
     */
    public static Set<String> getSchemeForValue(String value) {
        Set<String> schemes = new HashSet<String>();
        Matcher matcher = SCHEME_PATTERN.matcher(value);
        while (matcher.find()) {
            String scheme = matcher.group(1);
            schemes.add(scheme);
            String remaining = matcher.group(2);
            if (remaining != null) {
                schemes.addAll(getSchemeForValue(remaining));
            }
        }
        return schemes;
    }

    /**
     * Extracts all placeholder resolver schemes that are referred from the configuration.
     * @param props
     * @return
     */
    public static Set<String> getSchemesForConfig(Map<String, String> props) {
        Set<String> schemes = new HashSet<String>();
        for (Map.Entry<String, String> e : props.entrySet()) {
            String value = e.getValue();
            schemes.addAll(getSchemeForValue(value));
        }
        return schemes;
    }

    /**
     * Extracts all placeholder resolver schemes that are referred from the profile.
     * @param configs
     * @return
     */
    public static Set<String> getSchemesForProfileConfigurations(Map<String, Map<String, String>> configs) {
        Set<String> schemes = new HashSet<String>();
        for (Map.Entry<String, Map<String, String>> e : configs.entrySet()) {
            Map<String, String> value = e.getValue();
            schemes.addAll(getSchemesForConfig(value));
        }
        return schemes;
    }

    /**
     * Waits for the all the {@link PlaceholderResolver} that correspond to the specified schemes, to become available.
     * @param schemes       The required schemes.
     * @param resolvers     A {@link ConcurrentMap} of schemes -> {@link DynamicReference} of {@link PlaceholderResolver}.
     * @return              The actual {@link Map} of schemes -> {@link PlaceholderResolver}.
     */
     public static Map<String, PlaceholderResolver> waitForPlaceHolderResolvers(ExecutorService executor, Set<String> schemes, Map<String, DynamicReference<PlaceholderResolver>> resolvers)  {
        final Map<String, PlaceholderResolver> result = new HashMap<String, PlaceholderResolver>();
        final Set<String> notFound = new HashSet<String>(schemes);
        CompletionService<PlaceholderResolver> completionService = new ExecutorCompletionService<PlaceholderResolver>(executor);
        for (String scheme : schemes) {
            completionService.submit(resolvers.get(scheme));
        }
        try {
            for (int i = 0; i < schemes.size(); i++) {
                try {
                    PlaceholderResolver resolver = completionService.take().get();
                    if (resolver != null) {
                        result.put(resolver.getScheme(), resolver);
                        notFound.remove(resolver.getScheme());
                    }
                } catch (ExecutionException ex) {
                   //ignore the exception here and throw an exception later reporting missing schemes.
                }
            }
        } catch (Exception ex) {
            throw new FabricException("Error while waiting for placeholder resolvers.", ex);
        }

        if (!notFound.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Missing Placeholder Resolvers:");
            for (String resolver : notFound) {
                sb.append(" ").append(resolver);
            }
            throw new FabricException(sb.toString());
        }
        return result;
    }

}
