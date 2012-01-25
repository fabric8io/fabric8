/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.generator;

/**
 *
 */
public class Log {

    public static org.apache.maven.plugin.logging.Log LOG = null;

    public static void debug(String msg, Object... args) {
        if ( LOG != null && LOG.isDebugEnabled() ) {
            LOG.debug(String.format(msg, args));
        }
    }

    public static void info(String msg, Object... args) {
        if ( LOG != null && LOG.isInfoEnabled() ) {
            LOG.info(String.format(msg, args));
        }
    }

    public static void warn(String msg, Object... args) {
        if ( LOG != null && LOG.isWarnEnabled() ) {
            LOG.warn(String.format(msg, args));
        }
    }

    public static void error(String msg, Object... args) {
        if ( LOG != null && LOG.isErrorEnabled() ) {
            LOG.error(String.format(msg, args));
        }
    }
}
