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

package org.fusesource.eca.expression;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Service;
import org.fusesource.eca.eventcache.CacheItem;

/**
 * Represents an expression
 */
public interface Expression extends Service {

    /**
     * If {@link Expression} is positive all the exchanges held by the event cache
     * which matches will be returned.
     *
     * @return A list of matching results, or <tt>null</tt> if no matching results.
     */
    List<CacheItem<Exchange>> getMatching();

    /**
     * Determines if currently matched.
     *
     * @return true if currently matched
     */
    boolean isMatch();

    /**
     * Validate the expression
     *
     * @param context the camel context
     */
    void validate(CamelContext context);

    /**
     * Gets the key used to create the expression, or multiple, comma separated keys
     *
     * @return the key used to create the expression, or multiple, comma separated keys
     */
    String getFromIds();

}
