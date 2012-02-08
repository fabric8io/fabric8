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

package org.fusesource.eca.component.eca;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.seda.SedaConsumer;
import org.fusesource.eca.engine.ExpressionListener;
import org.fusesource.eca.expression.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EcaConsumer extends SedaConsumer implements ExpressionListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(EcaConsumer.class);

    public EcaConsumer(EcaEndpoint ecaEndpoint, Processor processor) {
        super(ecaEndpoint, processor);
    }

    @Override
    public EcaEndpoint getEndpoint() {
        return (EcaEndpoint) super.getEndpoint();
    }

    /**
     * Evaluates the exchange
     */
    protected void sendToConsumers(Exchange exchange) throws Exception {
        // ensure route id is correct set due CAMEL-4806
        // TODO: Remove this when CAMEL-4806 is in released Fuse Camel version
        // and override prepareExchange method instead and set from route id detail there
        String routeId = (String) exchange.getIn().removeHeader("EcaRouteId");
        if (exchange.getFromRouteId() == null) {
            exchange.setFromRouteId(routeId);
        }

        getEndpoint().evaluate(exchange);
    }

    /**
     * Sends the exchange to the consumers of this endpoint.
     */
    protected void doSendToConsumers(Exchange exchange) throws Exception {
        super.sendToConsumers(exchange);
    }

    /**
     * Implementation of ExpressionListener
     */
    public void expressionFired(Expression expression, Exchange exchange) {
        try {
            doSendToConsumers(exchange);
        } catch (Exception e) {
            LOG.warn("Failed to send to consumers. This exception will be ignored.", e);
        }
    }

    protected void doStart() throws Exception {
        getEndpoint().addExpression(this);
        super.doStart();
    }

    protected void doStop() throws Exception {
        super.doStop();
        getEndpoint().removeExpression(this);
    }

}
