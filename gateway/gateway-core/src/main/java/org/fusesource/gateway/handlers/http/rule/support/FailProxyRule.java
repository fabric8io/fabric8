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

import org.fusesource.gateway.handlers.http.rule.ProxyCommand;
import org.fusesource.gateway.handlers.http.rule.ProxyOperation;
import org.fusesource.gateway.handlers.http.rule.ProxyRule;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class FailProxyRule implements ProxyRule {

    private final Pattern from;
    private final ProxyOperation commandType;

    public FailProxyRule(Pattern from, ProxyOperation commandType) {
        this.from = from;
        this.commandType = commandType;
    }

    public FailProxyRule(String fromRegex) {
        this(fromRegex, ProxyOperation.Fail);
    }

    public FailProxyRule(String fromRegex, ProxyOperation commandType) {
        this(Pattern.compile(fromRegex), commandType);
    }

    @Override
    public String toString() {
        return "FailProxyRule{" +
                "from=" + from +
                ", commandType=" + commandType +
                '}';
    }

    @Override
    public ProxyCommand apply(HttpServerRequest request) {
        String uri = request.uri();
        Matcher matcher = from.matcher(uri);
        if (matcher.matches()) {
            return new ProxyCommand(commandType, null);
        }
        return null;
    }

}
