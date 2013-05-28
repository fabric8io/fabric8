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

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.processor.DelegateAsyncProcessor;

import java.util.Map;

/**
 *
 */
public class ProfilerProcessor extends DelegateAsyncProcessor {
    private final Profiler profiler;
    private final Stats stats;
    private final Map<String, ExchangeData> exchanges;

    public ProfilerProcessor(Profiler profiler, Processor processor, Stats stats, Map<String, ExchangeData> exchanges) {
        super(processor);
        this.profiler = profiler;
        this.stats = stats;
        this.exchanges = exchanges;
    }

    @Override
    public boolean process(final Exchange exchange, final AsyncCallback callback) {
        if (!profiler.isEnabled(exchange)) {
            return processor.process(exchange, callback);
        }
        ExchangeData data = exchanges.get(exchange.getExchangeId());
        if (data == null) {
            data = new ExchangeData();
            exchanges.put(exchange.getExchangeId(), data);
            exchange.addOnCompletion(data);
        }
        final ExchangeData ed = data;
        ed.start(stats);
        try {
            return processor.process(exchange, new AsyncCallback() {
                @Override
                public void done(boolean doneSync) {
                    ed.start(stats);
                    try {
                        callback.done(doneSync);
                    } finally {
                        if (ed.stop(stats)) {
                            exchanges.remove(exchange.getExchangeId());
                        }
                    }
                }
            });
        } finally {
            if (ed.stop(stats)) {
                exchanges.remove(exchange.getExchangeId());
            }
        }
    }

    @Override
    public String toString() {
        return "Profiler[" + processor + "]";
    }
}
