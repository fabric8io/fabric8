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
package org.fusesource.gateway.handlers.http.rule.support;

import org.fusesource.common.util.Strings;
import org.fusesource.gateway.handlers.http.rule.ProxyCommand;
import org.fusesource.gateway.handlers.http.rule.ProxyOperation;
import org.fusesource.gateway.handlers.http.rule.ProxyRule;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uses a regex to match the incoming URL
 */
public class DefaultProxyRule implements ProxyRule {

    private final Pattern from;
    private final ProxyOperation commandType;
    private final String toPrefix;

    public DefaultProxyRule(Pattern from, String toPrefix, ProxyOperation commandType) {
        this.from = from;
        this.commandType = commandType;
        this.toPrefix = toPrefix;
    }

    public DefaultProxyRule(String fromRegex, String toPrefix) {
        this(fromRegex, toPrefix, ProxyOperation.Proxy);
    }

    public DefaultProxyRule(String fromRegex, String toPrefix, ProxyOperation commandType) {
        this(Pattern.compile(fromRegex), toPrefix, commandType);
    }

    @Override
    public String toString() {
        return "DefaultProxyRule{" +
                "from=" + from +
                ", toPrefix='" + toPrefix + '\'' +
                ", commandType=" + commandType +
                '}';
    }

    @Override
    public ProxyCommand apply(HttpServerRequest request) {
        String uri = request.uri();
        Matcher matcher = from.matcher(uri);
        if (matcher.matches()) {
            String actualUrl = createProxyUrl(uri, request, matcher);
            return new ProxyCommand(commandType, actualUrl);
        }
        return null;
    }

    protected String createProxyUrl(String uri, HttpServerRequest request, Matcher matcher) {
        String remaining = matcher.group(1);
        if (Strings.isNotBlank(remaining)) {
            return toPrefix + remaining;
        } else {
            return toPrefix;
        }
    }
}
