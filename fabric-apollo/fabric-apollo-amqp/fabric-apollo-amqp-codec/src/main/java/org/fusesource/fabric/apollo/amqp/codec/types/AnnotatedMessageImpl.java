/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec.types;

import org.fusesource.fabric.apollo.amqp.codec.api.AnnotatedMessage;
import org.fusesource.fabric.apollo.amqp.codec.api.BareMessage;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

import java.io.DataOutput;

/**
 *
 */
public class AnnotatedMessageImpl implements AnnotatedMessage {

    protected Header header;
    protected DeliveryAnnotations deliveryAnnotations;
    protected MessageAnnotations messageAnnotations;
    protected BareMessageImpl message;
    protected Footer footer;

    public Buffer encode() throws Exception {
        int size = (int)size();
        if (size == 0) {
            return new Buffer(0);
        }
        DataByteArrayOutputStream out = new DataByteArrayOutputStream(size);
        write(out);
        return out.toBuffer();
    }

    public void write(DataOutput out) throws Exception {
        if (header != null) {
            header.write(out);
        }
        if (deliveryAnnotations != null) {
            deliveryAnnotations.write(out);
        }
        if (messageAnnotations != null) {
            messageAnnotations.write(out);
        }
        if (message != null) {
            message.write(out);
        }
        if (footer != null) {
            footer.write(out);
        }
    }

    public long size() {
        long rc = 0;
        if (header != null) {
            rc += header.size();
        }
        if (deliveryAnnotations != null) {
            rc += deliveryAnnotations.size();
        }
        if (messageAnnotations != null) {
            rc += messageAnnotations.size();
        }
        if (message != null) {
            rc += message.size();
        }
        if (footer != null) {
            rc += footer.size();
        }
        return rc;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        if (header != null) {
            buf.append("\n");
            buf.append(header.toString());
        }
        if (deliveryAnnotations != null) {
            buf.append("\n");
            buf.append(deliveryAnnotations.toString());
        }
        if (messageAnnotations != null) {
            buf.append("\n");
            buf.append(messageAnnotations.toString());
        }
        if (message != null) {
            buf.append("\n");
            buf.append(message.toString());
        }
        if (footer != null) {
            buf.append("\n");
            buf.append(footer.toString());
        }
        buf.append("\n");
        return buf.toString().trim();
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public DeliveryAnnotations getDeliveryAnnotations() {
        return deliveryAnnotations;
    }

    public void setDeliveryAnnotations(DeliveryAnnotations deliveryAnnotations) {
        this.deliveryAnnotations = deliveryAnnotations;
    }

    public MessageAnnotations getMessageAnnotations() {
        return messageAnnotations;
    }

    public void setMessageAnnotations(MessageAnnotations messageAnnotations) {
        this.messageAnnotations = messageAnnotations;
    }

    public BareMessage getMessage() {
        return message;
    }

    public void setMessage(BareMessage message) {
        this.message = (BareMessageImpl)message;
    }

    public Footer getFooter() {
        return footer;
    }

    public void setFooter(Footer footer) {
        this.footer = footer;
    }
}
