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
package org.fusesource.gateway.handlers.http.rule;

import org.vertx.java.core.http.HttpServerRequest;

/**
 * A URL matching rule
 */
public interface ProxyRule {

    /**
     * Tries to apply this rule if its applicable returning the action
     * or return null if this rule is not applicable.
     *
     * @param request the HTTP request
     * @return the action or null if this rule is not applicable
     */
    public ProxyCommand apply(HttpServerRequest request);

}
