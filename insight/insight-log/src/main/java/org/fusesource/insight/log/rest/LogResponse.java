/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.insight.log.rest;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class LogResponse {
	private LogResponseHits hits;

	@JsonCreator
	public LogResponse(@JsonProperty("hits") LogResponseHits hits) {
		this.hits = hits;
	}

	public LogResponseHits getHits() {
		return hits;
	}

	public void setHits(LogResponseHits hits) {
		this.hits = hits;
	}


}
