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

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.AmqpEncodingError;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.AmqpMarshaller;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.Encoded;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;

import static org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory.createAmqpBoolean;
import static org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory.createAmqpNull;

/**
 * Wrapper to handle fields with value="true", which is encoded with a descriptor and then a list type.
 */
public interface Multiple extends AmqpList {

    public void setValue(AmqpType<?, ?> value);
    public AmqpType<?, ?> getValue();

    public static class MultipleBean implements Multiple {

        private MultipleBuffer buffer;
        private MultipleBean bean = this;
        private AmqpType<?, ?> value;

        MultipleBean() {

        }

        MultipleBean(IAmqpList<AmqpType<?, ?>> value) {
            for ( int i = 0; i < value.getListCount(); i++ ) {
                set(i, value.get(i));
            }
        }

        MultipleBean(Multiple.MultipleBean other) {
            this.bean = other;
        }

        public final MultipleBean copy() {
            return new Multiple.MultipleBean(bean);
        }

        public final MultipleBuffer getBuffer(AmqpMarshaller marshaller) throws AmqpEncodingError {
            if(buffer == null) {
                buffer = new MultipleBuffer(marshaller.encode(this));
            }
            return buffer;
        }

        public final void marshal(DataOutput out, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError{
            if ( bean.value == null ) {
                createAmqpNull(null).getBuffer(marshaller).marshal(out, marshaller);
                return;
            }
            if ( bean.value instanceof AmqpList ) {
                out.write(0x0);
                createAmqpBoolean(true).marshal(out, marshaller);
            }
            bean.value.marshal(out, marshaller);
        }

        public void setValue(AmqpType<?, ?> value) {
            //copyCheck();
            bean.value = value;
        }

        public AmqpType<?, ?> getValue() {
            return bean.value;
        }

        public void set(int index, AmqpType<?, ?> value) {
            switch(index) {
                case 0:
                    setValue(value);
                    break;
                default : {
                    throw new IndexOutOfBoundsException(String.valueOf(index));
                }
            }
        }

        public AmqpType<?, ?> get(int index) {
            switch(index) {
                case 0:
                    return bean.value;
                default : {
                    throw new IndexOutOfBoundsException(String.valueOf(index));
                }
            }
        }

        public int getListCount() {
            return 1;
        }

        public Iterator<AmqpType<?, ?>> iterator() {
            return new AmqpListIterator<AmqpType<?, ?>>(bean);
        }

        public String toString() {
            String ret = "Multiple: ";
            if ( value != null ) {
                ret += "value=" + value + " ";
            }
            return ret;
        }

        private void copyCheck() {
            if(buffer != null) {
                throw new IllegalStateException("unwriteable");
            }
            if(bean != this) {
                copy(bean);
            }
        }

        public void copy(Multiple.MultipleBean other) {
            bean = this;
        }

        public boolean equals(Object o){
            if(this == o) {
                return true;
            }

            if(o == null || !(o instanceof Multiple)) {
                return false;
            }

            return equals((Multiple) o);
        }

        public boolean equals(Multiple b) {
            if(b.getValue() == null ^ getValue() == null) {
                return false;
            }
            if(b.getValue() != null && !b.getValue().equals(getValue())){
                return false;
            }
            return true;
        }

        public int hashCode() {
            return AbstractAmqpList.hashCodeFor(this);
        }
    }

    public static class MultipleBuffer extends AmqpList.AmqpListBuffer implements Multiple {

        private MultipleBean bean;

        protected MultipleBuffer(Encoded<IAmqpList<AmqpType<?, ?>>> encoded) {
            super(encoded);
        }

        public final void setValue(AmqpType<?, ?> value) {
            bean().setValue(value);
        }

        public final AmqpType<?, ?> getValue() {
            return bean().getValue();
        }

        protected Multiple bean() {
            if ( bean == null ) {
                bean = new MultipleBean(encoded.getValue());
                bean.buffer = this;
            }
            return bean;
        }

        public boolean equals(Object o){
            return bean().equals(o);
        }

        public boolean equals(AmqpOpen o){
            return bean().equals(o);
        }

        public int hashCode() {
            return bean().hashCode();
        }

        public static Multiple.MultipleBuffer create(Encoded<IAmqpList<AmqpType<?, ?>>> encoded) {
            if(encoded.isNull()) {
                return null;
            }
            return new Multiple.MultipleBuffer(encoded);
        }

        public static Multiple.MultipleBuffer create(DataInput in, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError {
            return create(marshaller.unmarshalMultiple(in));
        }

        public static Multiple.MultipleBuffer create(Buffer buffer, int offset, AmqpMarshaller marshaller) throws AmqpEncodingError {
            return create(marshaller.decodeMultiple(buffer, offset));
        }

    }
}
