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
package io.fabric8.api.jmx;

/**
 */
public interface ZooKeeperFacadeMXBean {
    /**
     * Reads the contents of a path
     */
    ZkContents read(String path) throws Exception;

    /**
     * Returns the content of the given path, assuming the path refers to a file
     */
    String getContents(String path) throws Exception;

/*
    void write(String path, String commitMessage,
               String authorName, String authorEmail, String contents);

    void remove(String branch, String path, String commitMessage,
                String authorName, String authorEmail);
*/
}
