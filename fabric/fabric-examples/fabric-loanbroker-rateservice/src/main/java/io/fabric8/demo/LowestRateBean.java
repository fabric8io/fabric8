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
package io.fabric8.demo;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LowestRateBean implements AggregationStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(LowestRateBean.class);

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        String newBank = newExchange.getIn().getHeader("host.bank", String.class);
        Double newRate = newExchange.getIn().getHeader("loan.rate", Double.class);

        LOG.info("Received rate {} from {}", newRate, newBank);

        if (oldExchange == null) {
            LOG.info("New lowest rate {} from {}", newRate, newBank);
            return newExchange;
        }

        String oldBank = oldExchange.getIn().getHeader("host.bank", String.class);
        Double oldRate = oldExchange.getIn().getHeader("loan.rate", Double.class);

        int winner = oldRate.compareTo(newRate);
        boolean newWinner = winner > 0;
        if (newWinner) {
            LOG.info("New lowest rate {} from {}", newRate, newBank);
            return newExchange;
        } else {
            LOG.info("Keeping lowest rate {} from {}", oldRate, oldBank);
            return oldExchange;
        }
    }

}
