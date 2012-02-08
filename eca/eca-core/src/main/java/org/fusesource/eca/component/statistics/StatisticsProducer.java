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

package org.fusesource.eca.component.statistics;

import java.util.concurrent.BlockingQueue;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.WaitForTaskToComplete;
import org.apache.camel.component.seda.SedaProducer;
import org.fusesource.eca.processor.StatisticsProcessor;

public class StatisticsProducer extends SedaProducer {

    public StatisticsProducer(StatisticsEndpoint endpoint, BlockingQueue<Exchange> queue, WaitForTaskToComplete waitForTaskToComplete, long timeout) {
        super(endpoint, queue, waitForTaskToComplete, timeout);
    }

    @Override
    public StatisticsEndpoint getEndpoint() {
        return (StatisticsEndpoint) super.getEndpoint();
    }

    public boolean process(final Exchange exchange, final AsyncCallback callback) {
        Object result = null;
        if (!StatisticsProcessor.isAlreadyProcessedForStatistics(exchange)) {
            try {
                result = getEndpoint().getStatsProcessor().processExchange(exchange);
            } catch (Throwable e) {
                exchange.setException(e);
            }
            if (result != null) {
                if (exchange.getPattern().isOutCapable()) {
                    exchange.getOut().setBody(result);
                } else {
                    exchange.getIn().setBody(result);
                }
            }
        }

        return super.process(exchange, callback);
    }
}
