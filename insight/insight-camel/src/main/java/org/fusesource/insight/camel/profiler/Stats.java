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
package org.fusesource.insight.camel.profiler;

import org.apache.camel.model.ProcessorDefinition;

import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class Stats {
    final AtomicLong count = new AtomicLong();
    final AtomicLong self = new AtomicLong();
    final AtomicLong total = new AtomicLong();
    final Stats parent;
    final ProcessorDefinition definition;

    public Stats(ProcessorDefinition definition, Stats parent) {
        this.definition = definition;
        this.parent = parent;
    }

    public synchronized void addTime(long self, long total) {
        this.count.incrementAndGet();
        this.self.addAndGet(self);
        this.total.addAndGet(total);
    }

    public Stats getParent() {
        return parent;
    }

    public long getCount() {
        return count.get();
    }

    public long getSelf() {
        return self.get();
    }

    public long getTotal() {
        return total.get();
    }

    public void reset() {
        count.set(0);
        self.set(0);
        total.set(0);
    }

    public String toString() {
        return "Stats[" + definition.toString() + "]";
    }
}
