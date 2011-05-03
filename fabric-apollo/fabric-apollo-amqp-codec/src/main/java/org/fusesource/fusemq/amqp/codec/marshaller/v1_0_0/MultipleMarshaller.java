/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.codec.marshaller.v1_0_0;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.fusemq.amqp.codec.marshaller.AmqpEncodingError;
import org.fusesource.fusemq.amqp.codec.marshaller.Encoded;
import org.fusesource.fusemq.amqp.codec.types.*;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.fusesource.fusemq.amqp.codec.types.IAmqpList.AmqpWrapperList;
import static org.fusesource.fusemq.amqp.codec.types.TypeFactory.*;

/**
 * Marshaller for Multiple wrapper type
 */
public class MultipleMarshaller implements DescribedTypeMarshaller<Multiple> {

    static final MultipleMarshaller SINGLETON = new MultipleMarshaller();
    private static final Encoded<IAmqpList<AmqpType<?, ?>>> NULL_ENCODED = new NullEncoded<IAmqpList<AmqpType<?, ?>>>();

    // made up, but used in the code generator
    public static final String SYMBOLIC_ID = "amqp:multiple:list";
    public static final long CATEGORY = 0;
    public static final long DESCRIPTOR_ID = 0x41;
    public static final long NUMERIC_ID = 0x41;


    public static final EncodedBuffer DESCRIPTOR = FormatCategory.createBuffer(new Buffer(new byte[]{0x41}), 0);

    private static final ListDecoder<AmqpType<?, ?>> DECODER = new ListDecoder<AmqpType<?, ?>>() {
        public final IAmqpList<AmqpType<?, ?>> unmarshalType(int dataCount, int dataSize, DataInput in) throws AmqpEncodingError, IOException {
            IAmqpList<AmqpType<?, ?>> rc = new IAmqpList.ArrayBackedList<AmqpType<?, ?>>(new AmqpType<?, ?>[1]);
            if ( dataCount > 1) {
                List<AmqpType<?, ?>> arg = new ArrayList<AmqpType<?, ?>>();
                for ( int i=0; i < dataCount; i++ ) {
                    arg.add(AmqpMarshaller.SINGLETON.unmarshalType(in));
                }
                rc.set(0, createAmqpList(new AmqpWrapperList(arg)));
            } else {
                rc.set(0, AmqpMarshaller.SINGLETON.unmarshalType(in));
            }
            return rc;
        }

        public IAmqpList<AmqpType<?, ?>> decode(EncodedBuffer[] constituents) {
            IAmqpList<AmqpType<?, ?>> rc = new IAmqpList.ArrayBackedList<AmqpType<?, ?>>(new AmqpType<?, ?>[1]);
            if ( constituents.length > 1 ) {
                List<AmqpType<?, ?>> arg = new ArrayList<AmqpType<?, ?>>();
                for ( EncodedBuffer constituent : constituents ) {
                    arg.add(AmqpMarshaller.SINGLETON.decodeType(constituent));
                }
                rc.set(0, createAmqpList(new AmqpWrapperList(arg)));
            } else {
                rc.set(0, AmqpMarshaller.SINGLETON.decodeType(constituents[0]));
            }
            return rc;
        }
    };

    public static class MultipleEncoded extends DescribedEncoded<IAmqpList<AmqpType<?, ?>>> {
        private Multiple decodedValue = null;

        public MultipleEncoded(DescribedBuffer buffer) {
            super(buffer);
        }

        public MultipleEncoded(Multiple value) {
            super(AmqpListMarshaller.encode(value));
            decodedValue = value;
        }

        protected final String getSymbolicId() {
            throw new IllegalArgumentException("No symbolic ID for multiple");
        }

        protected final long getNumericId() {
            throw new IllegalArgumentException("No numeric ID for multiple");
        }

        protected final Encoded<IAmqpList<AmqpType<?, ?>>> decodeDescribed(EncodedBuffer encoded) throws AmqpEncodingError {
            return AmqpMarshaller.SINGLETON.decodeMultiple(encoded.getBuffer(), encoded.getBuffer().getOffset());
            //return AmqpListMarshaller.createEncoded(encoded, DECODER);
        }

        protected final Encoded<IAmqpList<AmqpType<?, ?>>> unmarshalDescribed(DataInput in) throws IOException {
            return AmqpListMarshaller.createEncoded(in, DECODER);
        }

        protected final EncodedBuffer getDescriptor() {
            return DESCRIPTOR;
        }

        public int getEncodedSize() {
            if ( decodedValue == null ) {
                return super.getEncodedSize();
            }
            if ( decodedValue.getValue() == null ) {
                return createAmqpNull(null).getBuffer(AmqpMarshaller.SINGLETON).getEncoded().getEncodedSize();
            }

            int rc = 0;
            if ( decodedValue.getValue() instanceof AmqpList ) {
                // described format code and "true" descriptor indicating it's a multiple
                rc += 2;
            }
            return rc + decodedValue.getValue().getBuffer(AmqpMarshaller.SINGLETON).getEncoded().getEncodedSize();
        }
    }

    public static final Encoded<IAmqpList<AmqpType<?, ?>>> encode(Multiple value) throws AmqpEncodingError {
        return new MultipleMarshaller.MultipleEncoded(value);
    }

    public static final Encoded<IAmqpList<AmqpType<?, ?>>> createEncoded(Buffer source, int offset) throws AmqpEncodingError {
        return createEncoded(FormatCategory.createBuffer(source, offset));
    }

    public static final Encoded<IAmqpList<AmqpType<?, ?>>> createEncoded(DataInput in) throws IOException, AmqpEncodingError {
        EncodedBuffer buffer = FormatCategory.createBuffer(in.readByte(), in);
        return createEncoded(buffer);
    }

    public static final Encoded<IAmqpList<AmqpType<?, ?>>> createEncoded(EncodedBuffer buffer) throws AmqpEncodingError {
        byte fc = buffer.getEncodingFormatCode();
        if (fc == Encoder.NULL_FORMAT_CODE) {
            return NULL_ENCODED;
        }
        if ( fc == Encoder.DESCRIBED_FORMAT_CODE ) {
            DescribedBuffer db = buffer.asDescribed();
            AmqpType<?, ?> descriptor = AmqpMarshaller.SINGLETON.decodeType(db.getDescriptorBuffer());
            if(!(descriptor instanceof AmqpBoolean)) {
                // it's a multiple with a single described type in it
                Multiple multiple = createMultiple();
                multiple.setValue(AmqpMarshaller.SINGLETON.decodeType(buffer));
                return new MultipleEncoded(multiple);
            }
            return new MultipleMarshaller.MultipleEncoded(db);
        }
        Multiple multiple = createMultiple();
        multiple.setValue(AmqpMarshaller.SINGLETON.decodeType(buffer));
        return new MultipleEncoded(multiple);
    }

    public final Multiple.MultipleBuffer decodeDescribedType(AmqpType<?, ?> descriptor, DescribedBuffer encoded) throws AmqpEncodingError {
        return Multiple.MultipleBuffer.create(new MultipleMarshaller.MultipleEncoded(encoded));
    }
}
