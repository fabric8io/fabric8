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

import java.util.concurrent.BlockingQueue;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.WaitForTaskToComplete;
import org.apache.camel.component.seda.SedaProducer;

public class EcaProducer extends SedaProducer {

    EcaEndpoint ecaEndpoint;

    public EcaProducer(EcaEndpoint endpoint, BlockingQueue<Exchange> queue, WaitForTaskToComplete waitForTaskToComplete, long timeout) {
        super(endpoint, queue, waitForTaskToComplete, timeout);
        this.ecaEndpoint = endpoint;
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        // TODO: Remove this when CAMEL-4806 is in released Fuse Camel version
        exchange.getIn().setHeader("EcaRouteId", exchange.getFromRouteId());
        return super.process(exchange, callback);
    }
}
