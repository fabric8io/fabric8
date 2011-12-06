/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper.internal;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic template for paths.
 *
 * @author ldywicki
 */
public class SimplePathTemplate {

    private static Pattern PATTERN = Pattern.compile("\\{([^/]+?)\\}");
    private List<String> parameters = new ArrayList<String>();
    private String path;

    public SimplePathTemplate(String path) {
        this.path = path;
        Matcher matcher = PATTERN.matcher(path);

        while (matcher.find()) {
            parameters.add(matcher.group(1));
        }
    }

    public List<String> getParameterNames() {
        return Collections.unmodifiableList(parameters);
    }

    public String bindByPosition(String ... params) {
        if (params.length != parameters.size()) {
            throw new IllegalArgumentException("Parameters mismatch. Path template contains " + parameters.size()
                + " parameters, " + params.length + " was given");
        }

        Map<String, String> paramsMap = new HashMap<String, String>();

        for (int i = 0, j = params.length; i < j; i++) {
            paramsMap.put(parameters.get(i), params[i]);
        }

        return bindByName(paramsMap);
    }

    public String bindByName(String ... params) {
        Map<String, String> paramsMap = new HashMap<String, String>();

        for (int i = 0, j = params.length; i < j; i += 2) {
            paramsMap.put(params[i], (i + 1 < j) ? params[i+1] : "");
        }

        return bindByName(paramsMap);
    }

    public String bindByName(Map<String, String> params) {
        if (params.size() != parameters.size()) {
            throw new IllegalArgumentException("Parameters mismatch. Path template contains " + parameters.size()
                + " parameters, " + params.size() + " was given");
        }

        String localPath = path;

        for (String key : params.keySet()) {
            if (!parameters.contains(key)) {
                throw new IllegalArgumentException("Unknown parameter " + key);
            }
            localPath = replace(localPath, key, params.get(key));
        }
        return localPath;
    }

    private String replace(String text, String key, String value) {
        if (value == null) {
            throw new NullPointerException("Parameter " + key + " is null.");
        }
        return text.replace("{" + key + "}", value);
    }
}
