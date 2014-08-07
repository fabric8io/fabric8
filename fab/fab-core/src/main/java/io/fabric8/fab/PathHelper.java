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
package io.fabric8.fab;

import java.util.logging.Logger;

public class PathHelper {
    private static final transient Logger LOG = Logger.getLogger(PathHelper.class.getName());

    public static String getUserMavenRepository() {
        String dir = System.getProperty("user.home", "~") + "/.m2/repository";
        LOG.fine("Using user repository: " + dir);
        return dir;
    }
}
