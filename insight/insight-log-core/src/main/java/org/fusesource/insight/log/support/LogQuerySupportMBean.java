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

package org.fusesource.insight.log.support;

import org.codehaus.jackson.JsonGenerationException;
import org.fusesource.insight.log.LogFilter;
import org.fusesource.insight.log.LogResults;

import java.io.IOException;

/**
 * An MBean for querying log events which has a serialized API and a simple JSON API to avoid serialization issues
 */
public interface LogQuerySupportMBean {

    /**
     * Returns all the available recent log events as a {@link LogResults} object which is then serialized
     *
     * @return the log events as a serialized object
     */
    public LogResults allLogResults() throws IOException;

    /**
     * Returns all the available log events since the given timestamp (millis)
     *
     * @return the log events as a serialized object
     */
    public LogResults logResultsSince(long time) throws IOException;

    /**
     * Returns the recent log events as a {@link LogResults} object which is then serialized
     *
     * @param count maximum number to return o <0 for all of them
     * @return the log events as a serialized object
     */
    public LogResults getLogResults(int count) throws IOException;

    /**
     * Queries the log results using the given filter
     *
     * @param filter the filter to apply to the logs
     * @return the log events as a serialized object
     */
    public LogResults queryLogResults(LogFilter filter);


    /**
     * Returns the source file for the given maven coordinates so that we can link log messages
     * to source code
     *
     * @param mavenCoordinates is a string of the form "groupId:artifactId:version".
     *                         For some uber bundles this can be a space separated list.
     */
    public String getSource(String mavenCoordinates, String className, String filePath) throws IOException;

    /**
     * Returns the javadoc file for the given maven coordinates and filePath
     *
     * @param mavenCoordinates is a string of the form "groupId:artifactId:version".
     *                         For some uber bundles this can be a space separated list.
     */
    public String getJavaDoc(String mavenCoordinates, String filePath) throws IOException;


    // JSON API

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

    /**
     * Allows a JSON filter to be specified then returns the log results as a serialised object
     */
    public LogResults jsonQueryLogResults(String jsonFilter) throws IOException;
}
