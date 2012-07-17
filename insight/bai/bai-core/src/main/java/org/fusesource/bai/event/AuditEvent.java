/*
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

package org.fusesource.bai.event;

import java.util.Date;

import org.apache.camel.Exchange;
import org.apache.camel.management.event.AbstractExchangeEvent;

/**
 * DTO that represents an AuditEvent
 * @author raul
 *
 */
public class AuditEvent extends AbstractExchangeEvent {
    private static final long serialVersionUID = 6818757465057171170L;
    
    public AuditEvent(Exchange source, AbstractExchangeEvent realEvent) {
        super(source);
        this.event = realEvent;
        this.timestamp = new Date();
    }
    
    public AbstractExchangeEvent event;
    public Date timestamp;
    public String endpointURI;
    public Exception exception;
    public String sourceContextId;
    public String sourceRouteId;
    public String breadCrumbId;
    public Boolean redelivered;
    public String currentRouteId;
    
}
