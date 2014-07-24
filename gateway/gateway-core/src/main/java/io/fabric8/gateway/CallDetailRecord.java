package io.fabric8.gateway;

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
