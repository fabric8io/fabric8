/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api.log.elastic;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.fusesource.fabric.api.log.LogEvent;

@JsonIgnoreProperties(ignoreUnknown=true)
public class LogResponseHit {
	@JsonProperty("_source")
	private LogEvent event;


	public LogResponseHit() {
	}

	public LogEvent getEvent() {
		return event;
	}

	public void setEvent(LogEvent event) {
		this.event = event;
	}

}
