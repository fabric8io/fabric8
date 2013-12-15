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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 */
public class MockProcessor implements Processor {
    private List<Path> processPaths = new CopyOnWriteArrayList<Path>();
    private List<Path> onRemovePaths = new CopyOnWriteArrayList<Path>();
    private long timeout = 10000;

    public void process(Path path) {
        processPaths.add(path);
    }

    public void onRemove(Path path) {
        onRemovePaths.add(path);
    }

    public List<Path> getProcessPaths() {
        return processPaths;
    }

    public List<Path> getOnRemovePaths() {
        return onRemovePaths;
    }


    public void expectProcessed(List<Expectation> expectations, File... files) throws Exception {
        expectProcessed(expectations, PathHelper.toPathArray(files));
    }

    public void expectProcessed(List<Expectation> expectations, Path... paths) throws Exception {
        for (final Path path : paths) {
            expectations.add(new Expectation() {
                public boolean isValid() {
                    return processPaths.contains(path);
                }

                public String toString() {
                    return "Should have processed the path " + path + " but was " + processPaths;
                }
            });
        }
    }

    public void expectRemoved(List<Expectation> expectations, File... files) throws Exception {
        expectRemoved(expectations, PathHelper.toPathArray(files));
    }

    public void expectRemoved(List<Expectation> expectations, Path... paths) throws Exception {
        for (final Path path : paths) {
            expectations.add(new Expectation() {
                public boolean isValid() {
                    return onRemovePaths.contains(path);
                }

                public String toString() {
                    return "Should have removed the path " + path + " but was " + onRemovePaths;
                }
            });
        }
    }


    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

}
