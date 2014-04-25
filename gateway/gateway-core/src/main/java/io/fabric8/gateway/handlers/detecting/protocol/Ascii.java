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
package io.fabric8.gateway.handlers.detecting.protocol;

import org.vertx.java.core.buffer.Buffer;

/**
 * Holds a string and it's ascii encoded value as a buffer.
 */
public class Ascii {

    private final Buffer buffer;
    private final String string;
    private int hashCode;

    public Ascii(Buffer buffer) {
        this.buffer = buffer;
        this.string = decode(buffer);
    }

    public Ascii(String string) {
        this.string = string;
        this.buffer = encode(string);
    }

    ///////////////////////////////////////////////////////////////////
    // Overrides
    ///////////////////////////////////////////////////////////////////

    public Buffer toBuffer() {
        return buffer;
    }

    public String toString() {
        return string;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;

        Class clazz = obj.getClass();
        if( clazz==Ascii.class ) {
            return obj.toString().equals(toString());
        }
        if( clazz==String.class ) {
            return obj.equals(toString());
        }
        if( clazz==Buffer.class ) {
            return obj.equals(toBuffer());
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = toString().hashCode();
        }
        return hashCode;
    }

    ///////////////////////////////////////////////////////////////////
    // Statics
    ///////////////////////////////////////////////////////////////////

    public static Ascii ascii(String value) {
        if (value == null) {
            return null;
        }
        return new Ascii(value);
    }

    public static Ascii ascii(Buffer buffer) {
        if (buffer == null) {
            return null;
        }
        return new Ascii(buffer);
    }

    static public Buffer encode(String value) {
        int size = value.length();
        Buffer rc = new Buffer(size);
        for (int i = 0; i < size; i++) {
            rc.appendByte((byte) (value.charAt(i) & 0xFF));
        }
        return rc;
    }

    static public String decode(Buffer value) {
        int size = value.length();
        char rc[] = new char[size];
        for (int i = 0; i < size; i++) {
            rc[i] = (char) (value.getByte(i) & 0xFF);
        }
        return new String(rc);
    }


}
