/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.insight.log.rest;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class LogResponseHits {
	private List<LogResponseHit> hits = new ArrayList<LogResponseHit>();

	@JsonCreator
	public LogResponseHits(@JsonProperty("hits") List<LogResponseHit> hits) {
		this.hits = hits;
	}

	public List<LogResponseHit> getHits() {
		return hits;
	}

	public void setHits(List<LogResponseHit> hits) {
		this.hits = hits;
	}

}
