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

import org.apache.camel.Exchange;
import org.apache.camel.spi.Synchronization;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 */
public class ExchangeData implements Synchronization {
    final Map<Stats, ProcessorData> data = new HashMap<Stats, ProcessorData>();
    final LinkedList<Stats> queue = new LinkedList<Stats>();
    long last;
    long level;
    boolean completed;

    public static class ProcessorData {
        long time;
        long total;
    }


    @Override
    public void onComplete(Exchange exchange) {
        completed = true;
        checkCompletedExchange();
    }

    @Override
    public void onFailure(Exchange exchange) {
        onComplete(exchange);
    }

    public void start(Stats stats) {
        long cur = System.nanoTime();
        Stats prev = queue.peek();
        if (prev != null) {
            addTime(prev, cur - last);
        }
        queue.addFirst(stats);
        last = cur;
        level++;
    }

    public boolean stop(Stats stats) {
        long cur = System.nanoTime();
        Stats ppd = queue.poll();
        assert stats == ppd;
        addTime(stats, cur - last);
        last = cur;
        level--;
        return checkCompletedExchange();
    }

    ProcessorData getData(Stats stats) {
        ProcessorData pd = data.get(stats);
        if (pd == null) {
            pd = new ProcessorData();
            data.put(stats, pd);
        }
        return pd;
    }

    void addTime(Stats stats, long time) {
        getData(stats).time += time;
        for (Stats s = stats; s != null; s = s.parent) {
            getData(s).total += time;
        }
    }

    boolean checkCompletedExchange() {
        if (completed && level == 0) {
            for (Map.Entry<Stats, ProcessorData> e : data.entrySet()) {
                e.getKey().addTime(e.getValue().time, e.getValue().total);
            }
            return true;
        }
        return false;
    }

}
