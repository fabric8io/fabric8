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
package io.fabric8.watcher.support;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.fusesource.common.util.Objects;
import io.fabric8.watcher.Processor;
import io.fabric8.watcher.Watcher;
import io.fabric8.watcher.WatcherListener;

/**
 */
public abstract class WatcherSupport implements Watcher {
    private List<WatcherListener> listeners = new CopyOnWriteArrayList<WatcherListener>();
    private Processor processor;

    @Override
    public Processor getProcessor() {
        return processor;
    }

    @Override
    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    public void addListener(WatcherListener listener) {
        Objects.notNull(listener, "listener");
        listeners.add(listener);
    }

    public void removeListener(WatcherListener listener) {
        listeners.remove(listener);
    }

    protected List<WatcherListener> getListeners() {
        return new ArrayList<WatcherListener>(listeners);
    }

    protected void fireListeners(Path child, WatchEvent.Kind kind) {
        List<WatcherListener> list = getListeners();
        for (WatcherListener listener : list) {
            listener.onWatchEvent(child, kind);
        }
    }
}
