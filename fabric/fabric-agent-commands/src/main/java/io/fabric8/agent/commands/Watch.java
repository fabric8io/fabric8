/**
 *
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
package io.fabric8.agent.commands;

import io.fabric8.agent.commands.support.ProfileVersionKey;
import io.fabric8.agent.commands.support.ProfileWatcher;
import io.fabric8.agent.mvn.Parser;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Command(name = "watch", scope = "fabric", description = "Watches and updates bundles.", detailedDescription = "classpath:watch.txt")
public class Watch extends OsgiCommandSupport {

    @Argument(index = 0, name = "urls", description = "The bundle URLs", required = false, multiValued = true)
    List<String> urls;

    @Option(name = "-i", aliases = {}, description = "Watch interval", required = false, multiValued = false)
    private long interval;

    @Option(name = "--start", description = "Starts watching the selected bundles", required = false, multiValued = false)
    protected boolean start;

    @Option(name = "--stop", description = "Stops watching all bundles", required = false, multiValued = false)
    protected boolean stop;

    @Option(name = "--remove", description = "Removes bundles from the watch list", required = false, multiValued = false)
    protected boolean remove;

    @Option(name = "--list", description = "Displays the watch list", required = false, multiValued = false)
    protected boolean list;

    @Option(name = "--no-upload", description = "If specified then updated bundles are not uploaded to the Fabric's maven proxy repository", required = false, multiValued = false)
    protected boolean noUpload;

    private ProfileWatcher watcher;

    @Override
    protected Object doExecute() throws Exception {

        if (start && stop) {
            System.err.println("Please use only one of --start and --stop options!");
            return null;
        }

        if (interval > 0) {
            System.out.println("Setting watch interval to " + interval + " ms");
            watcher.setInterval(interval);
        }
        if (stop) {
            System.out.println("Stopping watch");
            watcher.stop();
        }
        watcher.setUpload(!noUpload);
        if (urls != null) {
            if (remove) {
                for (String url : urls) {
                    watcher.remove(url);
                }
            } else {
                for (String url : urls) {
                    watcher.add(url);
                }
            }
        }
        if (start) {
            System.out.println("Starting watch");
            watcher.start();
        }

        if (list) { //List the watched bundles.
            String format = "%-40s %-30s %-8s %-80s";
            System.out.println(String.format(format, "URL", "Profile", "Version", "Bundle"));
            for (String url : watcher.getWatchURLs()) {

                Map<ProfileVersionKey, Map<String, Parser>> profileArtifacts = watcher.getProfileArtifacts();
                if (profileArtifacts.size() > 0) {
                    Set<Map.Entry<ProfileVersionKey, Map<String, Parser>>> entries = profileArtifacts.entrySet();
                    for (Map.Entry<ProfileVersionKey, Map<String, Parser>> entry : entries) {
                        ProfileVersionKey key = entry.getKey();
                        Map<String, Parser> artifactMap = entry.getValue();
                        Set<Map.Entry<String, Parser>> artifactMapEntries = artifactMap.entrySet();
                        for (Map.Entry<String, Parser> artifactMapEntry : artifactMapEntries) {
                            String location = artifactMapEntry.getKey();
                            Parser parser = artifactMapEntry.getValue();
                            if (watcher.isSnapshot(parser) || watcher.wildCardMatch(location, url)) {
                                System.out.println(String.format(format, url, key.getProfileId(), key.getVersion(), location));
                            }
                        }
                    }
                } else {
                    System.out.println(String.format(format, url, "", "", ""));
                }
            }
        } else {
            List<String> urls = watcher.getWatchURLs();
            if (urls != null && urls.size() > 0) {
                System.out.println("Watched URLs: ");
                for (String url : watcher.getWatchURLs()) {
                    System.out.println(url);
                }
            } else {
                System.out.println("No watched URLs");
            }
        }

        return null;
    }

    public ProfileWatcher getWatcher() {
        return watcher;
    }

    public void setWatcher(ProfileWatcher watcher) {
        this.watcher = watcher;
    }
}
