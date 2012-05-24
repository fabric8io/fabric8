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

import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.camel.Producer;
import org.apache.camel.management.PublishEventNotifier;
import org.apache.camel.management.event.ExchangeCompletedEvent;
import org.apache.camel.management.event.ExchangeCreatedEvent;
import org.apache.camel.management.event.ExchangeFailedEvent;
import org.apache.camel.management.event.ExchangeRedeliveryEvent;
import org.apache.camel.management.event.ExchangeSendingEvent;
import org.apache.camel.management.event.ExchangeSentEvent;
import org.apache.camel.util.ServiceHelper;

/**
 * { _id: <breadcrumbId>,
     exchanges: [
     { timestamp: <timestamp>,
        endpointUri: <uri>,
        in: <inMessage>,
        out: <outMessage>
     },
     { timestamp: <timestamp>,
        endpointUri: <uri>,
        in: <inMessage>,
        out: <outMessage>
     },
     { timestamp: <timestamp>,
        endpointUri: <uri>,
        in: <inMessage>,
        out: <outMessage>
     }
   ],
   
   failures: [
     { timestamp: <timestamp>,
       error: <exception and message>
     }
   ],
   
   redeliveries: 
     { endpoint: [timestamps],
       endpoint: [timestamps]
     }
   ],
   
   
}
 * @author raul
 *
 */
public class AuditEventNotifier extends PublishEventNotifier {

    // by default accept all
	private List<String> inRegex = Arrays.asList(".*");
	private List<String> outRegex = Arrays.asList(".*");
	private List<String> failureRegex = Arrays.asList(".*");
	private List<String> redeliveryRegex = Arrays.asList(".*");
	
	private Producer producer;

	public AuditEventNotifier() {
		setIgnoreCamelContextEvents(true);
		setIgnoreRouteEvents(true);
		setIgnoreServiceEvents(true);
	}
	
	@Override
	public boolean isEnabled(EventObject event) {
		if (!(event instanceof AuditEvent)) {
			return false;
		}
		
		AuditEvent ae = (AuditEvent) event;
		
		List<String> compareWith = null;
		if (ae.event instanceof ExchangeSendingEvent || ae.event instanceof ExchangeCreatedEvent) {
		    compareWith = inRegex;
		}
		else if (ae.event instanceof ExchangeSentEvent || ae.event instanceof ExchangeCompletedEvent) {
		    compareWith = outRegex;
		}
		else if (ae.event instanceof ExchangeRedeliveryEvent) {
		    compareWith = redeliveryRegex;
		}
		// logic if it's a failure is different; we compare against Exception
		else if (ae.event instanceof ExchangeFailedEvent) {
		    String exceptionClassName = ae.event.getExchange().getException().getClass().getCanonicalName();
	        return testRegexps(exceptionClassName, failureRegex);
		}
		// TODO: Failure handled
	    return compareWith == null ? false : testRegexps(ae.endpointURI, compareWith);
		
	}

    private boolean testRegexps(String endpointURI, List<String> regexps) {
		for (String regex : regexps) {
			if (endpointURI.matches(regex)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Substitute all arrays with CopyOnWriteArrayLists
	 */
	@Override
	protected void doStart() throws Exception {
		inRegex = new CopyOnWriteArrayList<String>(inRegex);
		outRegex = new CopyOnWriteArrayList<String>(outRegex);
		failureRegex = new CopyOnWriteArrayList<String>(failureRegex);
		redeliveryRegex = new CopyOnWriteArrayList<String>(redeliveryRegex);
	    
		super.doStart();
		
		producer = super.getEndpoint().createProducer();
		ServiceHelper.startService(producer);
	}

	public List<String> getInRegex() {
		return inRegex;
	}

	public void setInRegex(List<String> inRegex) {
		this.inRegex = inRegex;
	}

	public List<String> getOutRegex() {
		return outRegex;
	}

	public void setOutRegex(List<String> outRegex) {
		this.outRegex = outRegex;
	}

	public List<String> getFailureRegex() {
		return failureRegex;
	}

	public void setFailureRegex(List<String> failureRegex) {
		this.failureRegex = failureRegex;
	}

	public List<String> getRedeliveryRegex() {
		return redeliveryRegex;
	}

	public void setRedeliveryRegex(List<String> redeliveryRegex) {
		this.redeliveryRegex = redeliveryRegex;
	}
	


}
