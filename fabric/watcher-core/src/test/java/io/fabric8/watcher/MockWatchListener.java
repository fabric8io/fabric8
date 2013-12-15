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
package io.fabric8.watcher;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class MockWatchListener implements WatcherListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(MockWatchListener.class);

    private List<WatchEventValues> events = new CopyOnWriteArrayList<WatchEventValues>();

    public void onWatchEvent(Path path, WatchEvent.Kind kind) {
        LOG.info("onWatchEvent path: " + path + " kind: " + kind);
        WatchEventValues event = new WatchEventValues(path, kind);
        events.add(event);
    }


    protected WatchEventValues findEventForPath(Path path) {
        String pathText = path.toString();
        for (WatchEventValues event : events) {
            Path eventPath = event.getPath();
            String eventPathText = eventPath.toString();
            if (eventPath.equals(path) || eventPathText.equals(pathText)) {
                return event;
            }
        }
        return null;
    }

    public void expectCalledWith(List<Expectation> expectations, File... files) {
        expectCalledWith(expectations, PathHelper.toPathArray(files));
    }

    public void expectCalledWith(List<Expectation> expectations, Path... paths) {
        for (final Path path : paths) {
            expectations.add(new Expectation() {
                public boolean isValid() {
                    WatchEventValues event = findEventForPath(path);
                    return event != null;
                }

                public String toString() {
                    return "No change event on path " + path;
                }
            });
        }
    }

    public void expectNotCalledWith(List<Expectation> expectations, final Path path) {
        expectations.add(new Expectation() {
            private WatchEventValues event;

            public boolean isValid() {
                event = findEventForPath(path);
                return event == null;
            }

            public String toString() {
                return "Should not have received an event for " + path + " but got " + event;
            }
        });
    }

    public void clearEvents() {
        events.clear();
    }
}
