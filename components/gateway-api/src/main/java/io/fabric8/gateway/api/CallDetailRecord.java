/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.gateway.api;

import java.util.Date;

/**
 * Detailed Record of a Gateway call. This info can be used to report on gateway requests.
 */
public class CallDetailRecord {

	private final long callTimeNanos;
	private final String error;
	private final Date callDate;
	public CallDetailRecord(long callTimeNanos, String error) {
		super();
		this.callDate = new Date();
		this.callTimeNanos = callTimeNanos;
		this.error = error;
	}

	public long getCallTimeNanos() {
		return callTimeNanos;
	}
	
	public String getError() {
		return error;
	}
	
	public Date getCallDate() {
		return callDate;
	}
}
