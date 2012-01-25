/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.codec.marshaller;

import org.fusesource.fabric.apollo.amqp.codec.api.*;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPType;
import org.fusesource.fabric.apollo.amqp.codec.types.*;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayInputStream;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;

import static org.fusesource.fabric.apollo.amqp.codec.marshaller.TypeReader.*;

/**
 *
 */
public class MessageSupport {

    public static Buffer toBuffer(BareMessage message) throws Exception {
        if ( message == null || !(message instanceof BareMessageImpl) ) {
            return null;
        }
        return ((BareMessageImpl) message).toBuffer();
    }

    public static Buffer toBuffer(AnnotatedMessage message) throws Exception {
        if ( message == null || !(message instanceof AnnotatedMessageImpl) ) {
            return null;
        }
        return ((AnnotatedMessageImpl) message).toBuffer();
    }

    public static long size(BareMessage message) {
        if ( message == null ) {
            return 0;
        }
        return ((BareMessageImpl) message).size();
    }

    public static long size(AnnotatedMessage message) {
        if ( message == null ) {
            return 0;
        }
        return ((AnnotatedMessageImpl) message).size();
    }

    public static void write(AnnotatedMessage message, DataOutput out) throws Exception {
        if ( message == null ) {
            return;
        }
        ((AnnotatedMessageImpl) message).write(out);
    }

    public static void write(BareMessage message, DataOutput out) throws Exception {
        if ( message == null ) {
            return;
        }
        ((BareMessageImpl) message).write(out);
    }

    public static DataMessage createDataMessage(Object... args) {
        DataMessage rc = new DataMessageImpl();
        for ( Object arg : args ) {
            if ( arg instanceof Properties ) {
                rc.setProperties((Properties) arg);
            } else if ( arg instanceof ApplicationProperties ) {
                rc.setApplicationProperties((ApplicationProperties) arg);
            } else if ( arg instanceof Buffer ) {
                if ( rc.getData() == null ) {
                    rc.setData(new ArrayList<Data>());
                }
                rc.getData().add(new Data((Buffer) arg));
            } else if ( arg instanceof Data ) {
                if ( rc.getData() == null ) {
                    rc.setData(new ArrayList<Data>());
                }
                rc.getData().add((Data) arg);
            } else if ( arg instanceof List ) {
                rc.setData((List) arg);
            } else {
                throw new RuntimeException("Unknown type for DataMessage");
            }
        }
        return rc;
    }

    public static SequenceMessage createSequenceMessage(Object... args) {
        SequenceMessage rc = new SequenceMessageImpl();
        for ( Object arg : args ) {
            if ( arg instanceof Properties ) {
                rc.setProperties((Properties) arg);
            } else if ( arg instanceof ApplicationProperties ) {
                rc.setApplicationProperties((ApplicationProperties) arg);
            } else if ( arg instanceof AMQPSequence ) {
                if ( rc.getData() == null ) {
                    List list = new ArrayList();
                    list.add((AMQPSequence) arg);
                    rc.setData(list);
                } else {
                    rc.getData().add((AMQPSequence) arg);
                }
            } else if ( arg instanceof List ) {
                rc.setData((List) arg);
            } else {
                throw new RuntimeException("Unknown type for SequenceMessage");
            }
        }
        if ( rc == null ) {
            rc = new SequenceMessageImpl();
        }
        return rc;
    }

    public static ValueMessage createValueMessage(Object... args) {
        ValueMessage rc = new ValueMessageImpl();

        for ( Object arg : args ) {
            if ( arg instanceof Properties ) {
                rc.setProperties((Properties) arg);
            } else if ( arg instanceof ApplicationProperties ) {
                rc.setApplicationProperties((ApplicationProperties) arg);
            } else if ( arg instanceof AMQPValue ) {
                rc.setData((AMQPValue) arg);
            } else if ( arg instanceof AMQPType ) {
                ((ValueMessageImpl) rc).setData((AMQPType) arg);
            } else {
                throw new RuntimeException("Unknown type for ValueMessage");
            }

        }
        return rc;
    }

    public static AnnotatedMessage createAnnotatedMessage(Object... args) {
        AnnotatedMessage rc = new AnnotatedMessageImpl();

        for ( Object arg : args ) {
            if ( arg instanceof Header ) {
                rc.setHeader((Header) arg);
            } else if ( arg instanceof DeliveryAnnotations ) {
                rc.setDeliveryAnnotations((DeliveryAnnotations) arg);
            } else if ( arg instanceof MessageAnnotations ) {
                rc.setMessageAnnotations((MessageAnnotations) arg);
            } else if ( arg instanceof BareMessage ) {
                rc.setMessage((BareMessage) arg);
            } else if ( arg instanceof Footer ) {
                rc.setFooter((Footer) arg);
            }
        }

        return rc;
    }

    public static AnnotatedMessage readAnnotatedMessage(DataInput in) throws Exception {
        try {
            AnnotatedMessage rc = new AnnotatedMessageImpl();
            AMQPType type = TypeReader.read(in);
            if ( type == null ) {
                return rc;
            }
            BareMessage message = null;
            Properties properties = null;
            ApplicationProperties applicationProperties = null;

            while (type != null) {
                if ( type instanceof Header ) {
                    if ( rc.getHeader() != null ) {
                        throw new RuntimeException("More than one header section present in message");
                    }
                    rc.setHeader((Header) type);
                } else if ( type instanceof DeliveryAnnotations ) {
                    if ( rc.getDeliveryAnnotations() != null ) {
                        throw new RuntimeException("More than one delivery annotations section present in message");
                    }
                    rc.setDeliveryAnnotations((DeliveryAnnotations) type);
                } else if ( type instanceof MessageAnnotations ) {
                    if ( rc.getMessageAnnotations() != null ) {
                        throw new RuntimeException("More than one message annotations section present in message");
                    }
                    rc.setMessageAnnotations((MessageAnnotations) type);
                } else if ( type instanceof Data ) {
                    if ( message != null && !(message instanceof DataMessageImpl) ) {
                        throw new RuntimeException("More than one type of application data section present in message");
                    }
                    if ( message == null ) {
                        message = createDataMessage((Data) type);
                    } else {
                        ((DataMessage) message).getData().add((Data) type);
                    }
                } else if ( type instanceof AMQPSequence ) {
                    if ( message != null && !(message instanceof SequenceMessageImpl) ) {
                        throw new RuntimeException("More than one type of application data section present in message");
                    }
                    if ( message == null ) {
                        message = createSequenceMessage((AMQPSequence) type);
                    } else {
                        ((SequenceMessage) message).getData().add((AMQPSequence) type);
                    }
                } else if ( type instanceof AMQPValue ) {
                    if ( message != null && !(message instanceof ValueMessageImpl) ) {
                        throw new RuntimeException("More than one type of application data section present in message");
                    }
                    if ( message == null ) {
                        message = createValueMessage((AMQPValue) type);
                    } else {
                        throw new RuntimeException("Only one instance of an AMQP value section can be present in a message");
                    }
                } else if ( type instanceof Properties ) {
                    if ( properties != null ) {
                        throw new RuntimeException("More than one properties section present in message");
                    }
                    properties = (Properties) type;
                } else if ( type instanceof ApplicationProperties ) {
                    if ( applicationProperties != null ) {
                        throw new RuntimeException("More than one application properties section present in message");
                    }
                    applicationProperties = (ApplicationProperties) type;
                } else if ( type instanceof Footer ) {
                    if ( rc.getFooter() != null ) {
                        throw new RuntimeException("More than one footer section present in message");
                    }
                    rc.setFooter((Footer) type);
                } else {
                    throw new RuntimeException("Unexpected section found in message : " + type);
                }

                type = TypeReader.read(in);
            }
            if ( message != null ) {
                message.setProperties(properties);
                message.setApplicationProperties(applicationProperties);
            }
            rc.setMessage(message);

            return rc;
        } catch (Exception e) {
            throw new RuntimeException("Error reading AnnotatedMessage : " + e.getMessage());
        }
    }

    private static <T extends AMQPType> T getSection(Buffer descriptor, Buffer buffer) throws Exception {
        int position = buffer.indexOf(descriptor);
        if ( position == -1 ) {
            return null;
        }
        DataByteArrayInputStream in = new DataByteArrayInputStream(buffer);
        while (position != -1) {
            try {
                in.setPos(position);
                return (T) TypeReader.read(in);
            } catch (Exception e) {

            }
            position = buffer.indexOf(descriptor, position + 1);
        }
        return null;
    }

    public static Header getHeader(Buffer buffer) throws Exception {
        if ( buffer.length == 0 ) {
            return null;
        }
        Header rc = null;
        rc = getSection(Header.NUMERIC_CONSTRUCTOR.getBuffer(), buffer);
        if ( rc == null ) {
            rc = getSection(Header.SYMBOLIC_CONSTRUCTOR.getBuffer(), buffer);
        }
        return rc;
    }

    public static MessageAnnotations getMessageAnnotations(Buffer buffer) throws Exception {
        if ( buffer.length == 0 ) {
            return null;
        }
        MessageAnnotations rc = null;
        rc = getSection(MessageAnnotations.NUMERIC_CONSTRUCTOR.getBuffer(), buffer);
        if ( rc == null ) {
            rc = getSection(MessageAnnotations.SYMBOLIC_CONSTRUCTOR.getBuffer(), buffer);
        }
        return rc;
    }

    public static DeliveryAnnotations getDeliveryAnnotations(Buffer buffer) throws Exception {
        if ( buffer.length == 0 ) {
            return null;
        }
        DeliveryAnnotations rc = null;
        rc = getSection(DeliveryAnnotations.NUMERIC_CONSTRUCTOR.getBuffer(), buffer);
        if ( rc == null ) {
            rc = getSection(DeliveryAnnotations.SYMBOLIC_CONSTRUCTOR.getBuffer(), buffer);
        }
        return rc;
    }

    public static Properties getProperties(Buffer buffer) throws Exception {
        if ( buffer.length == 0 ) {
            return null;
        }
        Properties rc = null;
        rc = getSection(Properties.NUMERIC_CONSTRUCTOR.getBuffer(), buffer);
        if ( rc == null ) {
            rc = getSection(Properties.SYMBOLIC_CONSTRUCTOR.getBuffer(), buffer);
        }
        return rc;
    }

    public static ApplicationProperties getApplicationProperties(Buffer buffer) throws Exception {
        if ( buffer.length == 0 ) {
            return null;
        }
        ApplicationProperties rc = null;
        rc = getSection(ApplicationProperties.NUMERIC_CONSTRUCTOR.getBuffer(), buffer);
        if ( rc == null ) {
            rc = getSection(ApplicationProperties.SYMBOLIC_CONSTRUCTOR.getBuffer(), buffer);
        }
        return rc;
    }

    //TODO - get body type scanners
    //TODO - scanners for body values, Data, AMQPValue, AMQPSequence

    public static Footer getFooter(Buffer buffer) throws Exception {
        if ( buffer.length == 0 ) {
            return null;
        }
        Footer rc = null;
        rc = getSection(Footer.NUMERIC_CONSTRUCTOR.getBuffer(), buffer);
        if ( rc == null ) {
            rc = getSection(Footer.SYMBOLIC_CONSTRUCTOR.getBuffer(), buffer);
        }
        return rc;
    }

    class SectionBoundary {
        String sectionName;
        long index;
        long size;
    }

    // TODO -- complete this...
    public static List<SectionBoundary> getSectionBoundaries(Buffer buffer) throws Exception {
        DataByteArrayInputStream in = new DataByteArrayInputStream(buffer);
        ArrayList<SectionBoundary> rc = new ArrayList<SectionBoundary>();
        while (in.available() != -1) {
            long index = in.getPos();
            byte formatCode = readFormatCode(in);
            if (checkEOS(formatCode)) {
                break;
            }
            if (formatCode != TypeRegistry.DESCRIBED_FORMAT_CODE) {
                throw new IllegalArgumentException("Invalid format code in message : " + formatCode);
            }
            AMQPType descriptor = readDescriptor(in);
            Class typeClass = getDescribedTypeClass(descriptor);

            if (typeClass == Data.class) {

            } else if (typeClass == AMQPValue.class) {

            } else if (typeClass == AMQPSequence.class) {

            } else {

            }





        }

        return rc;
    }

    public static AnnotatedMessage decodeAnnotatedMessage(Buffer buffer) throws Exception {
        return readAnnotatedMessage(new DataByteArrayInputStream(buffer));
    }

}
