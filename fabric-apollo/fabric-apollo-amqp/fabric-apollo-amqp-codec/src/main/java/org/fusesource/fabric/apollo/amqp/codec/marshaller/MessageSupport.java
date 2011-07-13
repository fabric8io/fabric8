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

import java.io.DataInput;
import java.io.DataOutput;

/**
 *
 */
public class MessageSupport {

    public static Buffer toBuffer(BareMessage message) throws Exception {
        if (message == null || !(message instanceof BareMessageImpl)) {
            return null;
        }
        return ((BareMessageImpl)message).toBuffer();
    }

    public static Buffer toBuffer(AnnotatedMessage message) throws Exception {
        if (message == null || !(message instanceof AnnotatedMessageImpl)) {
            return null;
        }
        return ((AnnotatedMessageImpl)message).toBuffer();
    }

    public static long size(BareMessage message) {
        if (message == null) {
            return 0;
        }
        return ((BareMessageImpl)message).size();
    }

    public static long size(AnnotatedMessage message) {
        if (message == null) {
            return 0;
        }
        return ((AnnotatedMessageImpl)message).size();
    }

    public static void write(AnnotatedMessage message, DataOutput out) throws Exception {
        if (message == null) {
            return;
        }
        ((AnnotatedMessageImpl)message).write(out);
    }

    public static void write(BareMessage message, DataOutput out) throws Exception {
        if (message == null) {
            return;
        }
        ((BareMessageImpl)message).write(out);
    }

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

    public static AnnotatedMessage readAnnotatedMessage(DataInput in) throws Exception {
        try {
            AnnotatedMessage rc = new AnnotatedMessageImpl();
            AmqpType type = TypeReader.read(in);
            if (type == null) {
                return rc;
            }
            BareMessage message = null;
            Properties properties = null;
            ApplicationProperties applicationProperties = null;

            while (type != null) {
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
                    ((ValueMessage)message).setData((AmqpValue)type);
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

                type = TypeReader.read(in);
            }
            if (message != null) {
                message.setProperties(properties);
                message.setApplicationProperties(applicationProperties);
            }
            rc.setMessage(message);

            return rc;
        } catch (Exception e) {
            throw new RuntimeException("Error reading AnnotatedMessage : " + e.getMessage());
        }
    }

    private static <T extends AmqpType> T getSection(Buffer descriptor, Buffer buffer) throws Exception {
        int position = buffer.indexOf(descriptor);
        if (position == -1) {
            return null;
        }
        DataByteArrayInputStream in = new DataByteArrayInputStream(buffer);
        while (position != -1) {
            try {
                in.setPos(position);
                return (T)TypeReader.read(in);
            } catch (Exception e) {

            }
            position = buffer.indexOf(descriptor, position + 1);
        }
        return null;
    }

    public static Header getHeader(Buffer buffer) throws Exception {
        if (buffer.length == 0) {
            return null;
        }
        Header rc = null;
        rc = getSection(Header.NUMERIC_CONSTRUCTOR.getBuffer(), buffer);
        if (rc == null) {
            rc = getSection(Header.SYMBOLIC_CONSTRUCTOR.getBuffer(), buffer);
        }
        return rc;
    }

    public static MessageAnnotations getMessageAnnotations(Buffer buffer) throws Exception {
        if (buffer.length == 0) {
            return null;
        }
        MessageAnnotations rc = null;
        rc = getSection(MessageAnnotations.NUMERIC_CONSTRUCTOR.getBuffer(), buffer);
        if (rc == null) {
            rc = getSection(MessageAnnotations.SYMBOLIC_CONSTRUCTOR.getBuffer(), buffer);
        }
        return rc;
    }

    public static DeliveryAnnotations getDeliveryAnnotations(Buffer buffer) throws Exception {
        if (buffer.length == 0) {
            return null;
        }
        DeliveryAnnotations rc = null;
        rc = getSection(DeliveryAnnotations.NUMERIC_CONSTRUCTOR.getBuffer(), buffer);
        if (rc == null) {
            rc = getSection(DeliveryAnnotations.SYMBOLIC_CONSTRUCTOR.getBuffer(), buffer);
        }
        return rc;
    }

    public static Properties getProperties(Buffer buffer) throws Exception {
        if (buffer.length == 0) {
            return null;
        }
        Properties rc = null;
        rc = getSection(Properties.NUMERIC_CONSTRUCTOR.getBuffer(), buffer);
        if (rc == null) {
            rc = getSection(Properties.SYMBOLIC_CONSTRUCTOR.getBuffer(), buffer);
        }
        return rc;
    }

    public static ApplicationProperties getApplicationProperties(Buffer buffer) throws Exception {
        if (buffer.length == 0) {
            return null;
        }
        ApplicationProperties rc = null;
        rc = getSection(ApplicationProperties.NUMERIC_CONSTRUCTOR.getBuffer(), buffer);
        if (rc == null) {
            rc = getSection(ApplicationProperties.SYMBOLIC_CONSTRUCTOR.getBuffer(), buffer);
        }
        return rc;
    }

    //TODO - get body type scanners
    //TODO - scanners for body values, Data, AmqpValue, AmqpSequence

    public static Footer getFooter(Buffer buffer) throws Exception {
        if (buffer.length == 0) {
            return null;
        }
        Footer rc = null;
        rc = getSection(Footer.NUMERIC_CONSTRUCTOR.getBuffer(), buffer);
        if (rc == null) {
            rc = getSection(Footer.SYMBOLIC_CONSTRUCTOR.getBuffer(), buffer);
        }
        return rc;
    }

    // TODO - need to have symbolic constructors in here as well...
    /*
    private static final Buffer[] ANNOTATED_MESSAGE_PARTS = new Buffer[] {
            Header.CONSTRUCTOR.getBuffer(),
            DeliveryAnnotations.CONSTRUCTOR.getBuffer(),
            MessageAnnotations.CONSTRUCTOR.getBuffer(),
            Properties.CONSTRUCTOR.getBuffer(),
            ApplicationProperties.CONSTRUCTOR.getBuffer(),
            Data.CONSTRUCTOR.getBuffer(),
            AmqpValue.CONSTRUCTOR.getBuffer(),
            AmqpSequence.CONSTRUCTOR.getBuffer(),
            Footer.CONSTRUCTOR.getBuffer()
    };

    private static final Buffer[] BARE_MESSAGE_PARTS = new Buffer[] {
            Properties.CONSTRUCTOR.getBuffer(),
            ApplicationProperties.CONSTRUCTOR.getBuffer(),
            Data.CONSTRUCTOR.getBuffer(),
            AmqpValue.CONSTRUCTOR.getBuffer(),
            AmqpSequence.CONSTRUCTOR.getBuffer()
    };
    */

    public static AnnotatedMessage decodeAnnotatedMessage(Buffer buffer) throws Exception {
        return readAnnotatedMessage(new DataByteArrayInputStream(buffer));
    }

}
