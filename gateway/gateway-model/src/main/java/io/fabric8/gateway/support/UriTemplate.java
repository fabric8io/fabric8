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
package io.fabric8.gateway.support;

import io.fabric8.gateway.model.HttpProxyRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple helper class for URI templates.
 */
public class UriTemplate {
    private static final Pattern PATTERN = Pattern.compile("\\{([^/]+?)\\}");
    private final String[] paths;
    private List<String> parameters = new ArrayList<String>();
    private String path;

    public UriTemplate(String path) {
        this.path = path;
        this.paths = Paths.splitPaths(path);
        Matcher matcher = PATTERN.matcher(path);

        while (matcher.find()) {
            parameters.add(matcher.group(1));
        }
    }

    public MappingResult matches(String[] requestUriPaths, HttpProxyRule proxyRule) {
        int actualLength = requestUriPaths.length;
        int processedPaths = 0;
        boolean joinedPath = false;
        Map<String, String> parameterNameValues = new HashMap<String, String>();
        for (int lastIndex = paths.length - 1; processedPaths <= lastIndex; processedPaths++) {
            String actualSegment = null;
            if (processedPaths < actualLength) {
                actualSegment = requestUriPaths[processedPaths];
            }
            if (actualSegment == null) {
                return null;
            }
            String parameterName = getWildcardParameterName(processedPaths);
            if (parameterName != null) {
                if (processedPaths == lastIndex) {
                    actualSegment = joinPath(processedPaths, requestUriPaths);
                    joinedPath = true;
                }
                parameterNameValues.put(parameterName, actualSegment);
            } else {
                String pathSegment = paths[processedPaths];
                if (pathSegment == null || !actualSegment.equals(pathSegment)) {
                    return null;
                }
            }
        }
        if (!joinedPath && processedPaths < actualLength) {
            return null;
        }
        return new MappingResult(parameterNameValues, requestUriPaths, proxyRule);
    }


    public List<String> getParameterNames() {
        return Collections.unmodifiableList(parameters);
    }

    public String bindByPosition(String... params) {
        if (params.length != parameters.size()) {
            throw new IllegalArgumentException("Parameters mismatch. Path template contains " + parameters.size()
                    + " parameters, " + params.length + " was given");
        }
        Map<String, String> paramsMap = new HashMap<String, String>();
        for (int i = 0, j = params.length; i < j; i++) {
            String param = params[i];
            if (param != null) {
                // lets remove trailing whitespace
                param = param.trim();
            }
            paramsMap.put(parameters.get(i), param);
        }
        return bindByName(paramsMap);
    }

    public String bindByName(String... params) {
        Map<String, String> paramsMap = new HashMap<String, String>();

        for (int i = 0, j = params.length; i < j; i += 2) {
            paramsMap.put(params[i], (i + 1 < j) ? params[i + 1] : "");
        }

        return bindByName(paramsMap);
    }

    public String bindByName(Map<String, String> params) {
        if (params.size() != parameters.size()) {
            throw new IllegalArgumentException("Parameters mismatch. Path template contains " + parameters.size()
                    + " parameters, " + params.size() + " was given");
        }

        String localPath = path;

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!parameters.contains(key)) {
                throw new IllegalArgumentException("Unknown parameter " + key);
            }
            localPath = replace(localPath, key, value);
        }
        return localPath;
    }

    /**
     * Like {@link #bindByName(java.util.Map)} but this method silently ignores any unnecessary parameters that are passed in.
     */
    public String bindByNameNonStrict(Map<String, String> params) {
        String localPath = path;
        for (String key : parameters) {
            String value = params.get(key);
            if (value != null) {
                localPath = replace(localPath, key, value);
            }
        }
        return localPath;
    }


    /**
     * Returns the wildcard parameter name for the given path index if its a wildcard otherwise return null if it is not a wildcard
     */
    protected String getWildcardParameterName(int pathIndex) {
        String parameterName = null;
        if (pathIndex >= 0 && pathIndex < paths.length) {
            String pathSegment = paths[pathIndex];
            if (pathSegment != null && pathSegment.startsWith("{") && pathSegment.endsWith("}")) {
                // we are a wildcard so lets expose the parameter value
                parameterName = pathSegment.substring(1, pathSegment.length() - 1);
            }
        }
        return parameterName;
    }

    /**
     * Returns the joined path with "/" from the given index until the end of the array of paths
     */
    protected String joinPath(int index, String[] paths) {
        int lastIndex = paths.length - 1;
        if (index == lastIndex) {
            return paths[index];
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = index; i <= lastIndex; i++) {
                String path = paths[i];
                if (path == null) {
                    path = "";
                }
                if (builder.length() > 0) {
                    builder.append("/");
                }
                builder.append(path);
            }
            return builder.toString();
        }
    }

    protected String replace(String text, String key, String value) {
        if (value == null) {
            throw new IllegalStateException("Parameter " + key + " is null.");
        }
        return text.replace("{" + key + "}", value);
    }

}
