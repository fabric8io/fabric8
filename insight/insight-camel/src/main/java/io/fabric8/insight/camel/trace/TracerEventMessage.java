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
package io.fabric8.insight.camel.trace;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 */
public class TracerEventMessage implements Serializable {

    public static final String ROOT_TAG = "tracerEventMessage";
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static final long serialVersionUID = 1L;

    private final long uid;
    private final Date timestamp;
    private final String toNode;
    private final String exchangeId;
    private final String messageAsXml;

    public TracerEventMessage(long uid, Date timestamp, String toNode, String exchangeId, String messageAsXml) {
        this.uid = uid;
        this.timestamp = timestamp;
        this.toNode = toNode;
        this.exchangeId = exchangeId;
        this.messageAsXml = messageAsXml;
    }

    public long getUid() {
        return uid;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getToNode() {
        return toNode;
    }

    public String getExchangeId() {
        return exchangeId;
    }

    public String getMessageAsXml() {
        return messageAsXml;
    }

    @Override
    public String toString() {
        return "TracerEvent[" + exchangeId + " at " + toNode + "]";
    }

    /**
     * Dumps the event message as XML using the {@link #ROOT_TAG} as root tag.
     * <p/>
     * The <tt>timestamp</tt> tag is formatted in the format defined by {@link #TIMESTAMP_FORMAT}
     *
     * @return xml representation of this event
     */
    public String toXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(ROOT_TAG).append(">\n");
        sb.append("<uid>").append(uid).append("</uid>\n");
        String ts = new SimpleDateFormat(TIMESTAMP_FORMAT).format(timestamp);
        sb.append("<timestamp>").append(ts).append("</timestamp>\n");
        sb.append("<toNode>").append(toNode).append("</toNode>\n");
        sb.append("<exchangeId>").append(exchangeId).append("</exchangeId>\n");
        sb.append(messageAsXml).append("\n");
        sb.append("</").append(ROOT_TAG).append(">");
        return sb.toString();
    }
}
