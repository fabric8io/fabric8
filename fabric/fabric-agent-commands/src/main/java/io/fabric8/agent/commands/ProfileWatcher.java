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
package io.fabric8.agent.commands;

import io.fabric8.agent.commands.support.ProfileVersionKey;
import io.fabric8.maven.util.Parser;

import java.util.List;
import java.util.Map;

public interface ProfileWatcher extends Runnable {

    void start();
    void stop();

    void setInterval(long interval);
    void setUpload(boolean upload);

    void add(String url);
    void remove(String url);
    List<String> getWatchURLs();

    Map<ProfileVersionKey, Map<String, Parser>> getProfileArtifacts();


    boolean wildCardMatch(String text);
    boolean wildCardMatch(String text, String pattern);


}
