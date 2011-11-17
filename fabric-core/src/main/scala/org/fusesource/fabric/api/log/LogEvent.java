/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api.log;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown=true)
public class LogEvent {
	private String host;
	private Long seq;
	private Date timestamp;
	private String level;
	private String logger;
	private String thread;
	private String message;
	private String[] exception;
	private Map<String, String> properties;
    private String className;
    private String fileName;
    private String methodName;
    private String lineNumber;

    public static LogEvent toLogEvent(Object element) {
		if (element instanceof LogEvent) {
			return (LogEvent) element;
		}
		return null;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Long getSeq() {
		return seq;
	}

	public void setSeq(Long seq) {
		this.seq = seq;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getLogger() {
		return logger;
	}

	public void setLogger(String logger) {
		this.logger = logger;
	}

	public String getThread() {
		return thread;
	}

	public void setThread(String thread) {
		this.thread = thread;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public String[] getException() {
		return exception;
	}

	public void setException(String[] exception) {
		this.exception = exception;
	}

    public String getClassName() {
        return className;
    }

    public String getFileName() {
        return fileName;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }
}
