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

package org.fusesource.insight.log.service;

import org.codehaus.jackson.JsonGenerationException;

import java.io.IOException;

/**
 * An MBean for querying log events
 */
public interface LogQueryMBean {

    /**
     * Returns the recent log events as JSON
     *
     * @param count maximum number to return o <0 for all of them
     * @return the log events as a blob of JSON using {@link org.fusesource.insight.log.LogEvent}
     */
    public String getLogEvents(int count) throws IOException;


    /**
     * Filters the list of log events using the JSON encoding of {@link org.fusesource.insight.log.LogFilter}
     *
     * @return the log events as a blob of JSON using {@link org.fusesource.insight.log.LogEvent}
     */
    public String filterLogEvents(String jsonFiler) throws IOException;

}
