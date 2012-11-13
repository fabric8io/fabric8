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

package org.fusesource.insight.log;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.fusesource.insight.log.support.Objects;

import java.util.Date;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown=true)
public class LogEvent implements Comparable<LogEvent> {
    private static String defaultContainerName;

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
    private String containerName;
    private String lineNumber;

    static {
        setDefaultContainerName(System.getProperty("karaf.name"));
    }

    public static LogEvent toLogEvent(Object element) {
		if (element instanceof LogEvent) {
			return (LogEvent) element;
		}
		return null;
	}

    public LogEvent() {
        this.containerName = getDefaultContainerName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogEvent logEvent = (LogEvent) o;

        if (host != null ? !host.equals(logEvent.host) : logEvent.host != null) return false;
        if (containerName != null ? !containerName.equals(logEvent.containerName) : logEvent.containerName != null) return false;
        if (logger != null ? !logger.equals(logEvent.logger) : logEvent.logger != null) return false;
        if (message != null ? !message.equals(logEvent.message) : logEvent.message != null) return false;
        if (seq != null ? !seq.equals(logEvent.seq) : logEvent.seq != null) return false;
        if (thread != null ? !thread.equals(logEvent.thread) : logEvent.thread != null) return false;
        if (timestamp != null ? !timestamp.equals(logEvent.timestamp) : logEvent.timestamp != null) return false;

        return true;
    }

    @Override
    public int compareTo(LogEvent that) {
        // use reverse order for timestamp and seq
        int answer = Objects.compare(this.timestamp, that.timestamp);
        if (answer == 0) {
            answer = Objects.compare(this.seq, that.seq);
            if (answer == 0) {
                answer = Objects.compare(this.host, that.host);
                if (answer == 0) {
                    answer = Objects.compare(this.containerName, that.containerName);
                    if (answer == 0) {
                        answer = Objects.compare(this.thread, that.thread);
                        if (answer == 0) {
                            answer = Objects.compare(this.logger, that.logger);
                            if (answer == 0) {
                                answer = Objects.compare(this.message, that.message);
                            }
                        }
                    }
                }
            }
        }
        return answer;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + (containerName != null ? containerName.hashCode() : 0);
        result = 31 * result + (seq != null ? seq.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (logger != null ? logger.hashCode() : 0);
        result = 31 * result + (thread != null ? thread.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }

    /**
     * A simple concise toString() for debugging purposes
     */
    public String toString() {
        return "[" + getLevel() + "] " + getMessage();
    }

    public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
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

    public static String getDefaultContainerName() {
        return defaultContainerName;
    }

    public static void setDefaultContainerName(String defaultContainerName) {
        LogEvent.defaultContainerName = defaultContainerName;
    }
}
