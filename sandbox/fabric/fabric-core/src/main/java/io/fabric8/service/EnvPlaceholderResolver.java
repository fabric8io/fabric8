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
package io.fabric8.service;

import java.util.Map;

import io.fabric8.api.FabricService;
import io.fabric8.api.PlaceholderResolver;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

@ThreadSafe
@Component(name = "io.fabric8.placholder.resolver.env", label = "Fabric8 Environment Placeholder Resolver", metatype = false)
@Service({ PlaceholderResolver.class, EnvPlaceholderResolver.class })
@Properties({ @Property(name = "scheme", value = EnvPlaceholderResolver.RESOLVER_SCHEME) })
public final class EnvPlaceholderResolver extends AbstractComponent implements PlaceholderResolver {

    public static final String RESOLVER_SCHEME = "env";
    public static final String ELVIS_OPERATOR = "?:";

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getScheme() {
        return RESOLVER_SCHEME;
    }

    @Override
    public String resolve(FabricService fabricService, Map<String, Map<String, String>> configs, String pid, String key, String value) {
        if (value != null && value.length() > RESOLVER_SCHEME.length()) {
            String name = value.substring(RESOLVER_SCHEME.length() + 1);
            int idx = name.indexOf(ELVIS_OPERATOR);
            String defaultValue = null;
            if (idx > 0) {
                defaultValue = name.substring(idx + ELVIS_OPERATOR.length());
                name = name.substring(0, idx);
            }
            String answer = System.getenv(name);
            if (answer == null) {
                answer = defaultValue;
            }
            return answer;
        }
        return value;
    }

    /**
     * Resolves an expression of the form "NAME" or "NAME?:DEFAULT" returning the original value if preserveUnresolved
     * is true and there is no environment variable defined yet
     */
    public static String resolveExpression(String expression, Map<String,String> environmentVariables, boolean preserveUnresolved) {
        int idx = expression.indexOf(ELVIS_OPERATOR);
        String defaultValue = null;
        String name = expression;
        if (idx > 0) {
            defaultValue = expression.substring(idx + ELVIS_OPERATOR.length());
            name = expression.substring(0, idx);
        }
        String answer = environmentVariables != null ? environmentVariables.get(name) : null;
        if (answer == null) {
            answer = System.getenv(name);
            if (answer == null) {
                return preserveUnresolved ? expression : defaultValue;
            }
        }
        return answer;
    }

    /**
     * Removes any ${env:XXX} token from the given expression which the {@link #resolveExpression(String, java.util.Map, boolean)}
     * method requires to be removed first.
     *
     * @param expression the expression
     * @return the expression with environment token removed
     */
    public static String removeTokens(String expression) {
        // remove placeholder tokens which the EnvPlaceholderResolver do not expect
        if (expression.startsWith("${") && expression.endsWith("}")) {
            expression = expression.substring(2, expression.length() - 1);
        }
        if (expression.startsWith("env:")) {
            expression = expression.substring(4);
        }
        return expression;
    }

}
