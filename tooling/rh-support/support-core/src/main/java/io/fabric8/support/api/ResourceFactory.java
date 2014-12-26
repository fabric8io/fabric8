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
package io.fabric8.support.api;

import java.io.File;

/**
 * Interface to allow for convenient creation of a few commonly used {@link io.fabric8.support.api.Resource} types
 */
public interface ResourceFactory {

    /**
     * Creates a {@link io.fabric8.support.api.Resource} that executes a Karaf command and captures the output
     *
     * @param command the Karaf shell command to execute
     * @return
     */
    public Resource createCommandResource(String command);

    /**
     * Creates a {@link io.fabric8.support.api.Resource} that captures the contents of a given file.
     *
     * @param file the file
     * @return
     */
    public Resource createFileResource(File file);

}
