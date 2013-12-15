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

package io.fabric8.cxf;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import java.io.IOException;

public class TransportFailureInterceptor extends AbstractPhaseInterceptor<Message> {

    public TransportFailureInterceptor() {
        super(Phase.POST_STREAM);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        String address = (String) message.get(Message.ENDPOINT_ADDRESS);
        System.out.println(" address is " + address);
        if (address.indexOf("fail") > 0) {
            Exception ex = new IOException("Throw the IOException for address " + address);
            message.setContent(Exception.class, ex);
            throw new Fault(ex);
        }
    }
}
