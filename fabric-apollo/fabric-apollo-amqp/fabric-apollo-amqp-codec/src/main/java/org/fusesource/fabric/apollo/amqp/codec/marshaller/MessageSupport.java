/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec.marshaller;

import org.fusesource.fabric.apollo.amqp.codec.api.*;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AmqpType;
import org.fusesource.fabric.apollo.amqp.codec.types.*;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayInputStream;

/**
 *
 */
public class MessageSupport {

    public static ValueMessage createValueMessage() {
        return new ValueMessageImpl();
    }

    public static SequenceMessage createSequenceMessage() {
        return new SequenceMessageImpl();
    }

    public static DataMessage createDataMessage() {
        return new DataMessageImpl();
    }

    public static AnnotatedMessage createAnnotatedMessage() {
        return new AnnotatedMessageImpl();
    }

    public static Buffer encodeAnnotatedMessage(AnnotatedMessage message) throws Exception {
        if (message == null || !(message instanceof AnnotatedMessageImpl)) {
            return null;
        }
        return ((AnnotatedMessageImpl)message).encode();
    }

    public static AnnotatedMessage decodeAnnotatedMessage(Buffer buffer) throws Exception {
        DataByteArrayInputStream in = new DataByteArrayInputStream(buffer);

        AnnotatedMessage rc = new AnnotatedMessageImpl();
        BareMessage message = null;
        Properties properties = null;
        ApplicationProperties applicationProperties = null;

        while (in.available() > 0) {
            AmqpType type = TypeReader.read(in);
            if (type instanceof Header) {
                if (rc.getHeader() != null) {
                    throw new RuntimeException("More than one header section present in message");
                }
                rc.setHeader((Header)type);
            } else if (type instanceof DeliveryAnnotations) {
                if (rc.getDeliveryAnnotations() != null) {
                    throw new RuntimeException("More than one delivery annotations section present in message");
                }
                rc.setDeliveryAnnotations((DeliveryAnnotations)type);
            } else if (type instanceof MessageAnnotations ) {
                if (rc.getMessageAnnotations() != null) {
                    throw new RuntimeException("More than one message annotations section present in message");
                }
                rc.setMessageAnnotations((MessageAnnotations)type);
            } else if (type instanceof Data) {
                if (message != null && !(message instanceof DataMessageImpl)) {
                    throw new RuntimeException("More than one type of application data section present in message");
                }
                if (message == null) {
                    message = new DataMessageImpl();
                }
                ((DataMessage)message).getData().add((Data)type);
            } else if (type instanceof AmqpSequence) {
                if (message != null && !(message instanceof SequenceMessageImpl)) {
                    throw new RuntimeException("More than one type of application data section present in message");
                }
                if (message == null) {
                    message = new SequenceMessageImpl();
                }
                ((SequenceMessage)message).getData().add((AmqpSequence) type);
            } else if (type instanceof AmqpValue) {
                if (message != null && !(message instanceof ValueMessageImpl)) {
                    throw new RuntimeException("More than one type of application data section present in message");
                }
                if (message == null) {
                    message = new ValueMessageImpl();
                } else {
                    throw new RuntimeException("Only one instance of an AMQP value section can be present in a message");
                }
                ((ValueMessage)message).getData().setValue(type);
            } else if (type instanceof Properties) {
                if (properties != null) {
                    throw new RuntimeException("More than one properties section present in message");
                }
                properties = (Properties)type;
            } else if (type instanceof ApplicationProperties) {
                if (applicationProperties != null) {
                    throw new RuntimeException("More than one application properties section present in message");
                }
                applicationProperties = (ApplicationProperties)type;
            } else if (type instanceof Footer) {
                if (rc.getFooter() != null) {
                    throw new RuntimeException("More than one footer section present in message");
                }
                rc.setFooter((Footer)type);
            } else {
                throw new RuntimeException("Unexpected section found in message : " + type);
            }
        }

        if (message != null) {
            message.setProperties(properties);
            message.setApplicationProperties(applicationProperties);
        }
        rc.setMessage(message);

        return rc;
    }

}
