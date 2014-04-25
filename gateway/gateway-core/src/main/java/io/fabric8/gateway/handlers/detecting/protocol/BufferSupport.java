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

import io.netty.buffer.ByteBuf;
import org.vertx.java.core.buffer.Buffer;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * BufferSupport contains static methods that assist
 * with working with the vertx Buffer class.
 */
public class BufferSupport {

    static public Buffer chomp(Buffer self) {
        return self.getBuffer(0, self.length()-1);
    }

    static public Buffer trim(Buffer self) {
        return trimEnd(trimFront(self));
    }

    static public Buffer trimFront(Buffer self) {
        int length = self.length();
        int pos = 0;
        while ((pos < length) && (self.getByte(pos) <= ' ')) {
            pos++;
        }
        return (pos == 0) ? self : self.getBuffer(pos, length);
    }

    static public Buffer trimEnd(Buffer self) {
        int length = self.length();
        int pos = length;
        while ( pos > 0 && (self.getByte(pos-1) <= ' ')) {
            pos--;
        }
        return (pos == length-1) ? self : self.getBuffer(0, pos);
    }

    static public int indexOf(Buffer self, byte value) {
        return indexOf(self, 0, self.length(), value);
    }

    static public int indexOf(Buffer self, int start, byte value) {
        return indexOf(self, start, self.length(), value);
    }

    static public int indexOf(Buffer self, int start, int end, byte value) {
        int max = Math.min(end, self.length());
        for (; start < end ; start++) {
          if (self.getByte(start) == value ) {
              return start;
          }
        }
        return -1;
    }

    static public boolean startsWith(Buffer self, Buffer needle) {
        return indexOf(self, 0, needle.length(), needle) == 0;
    }

    static public boolean startsWith(Buffer self, int start, Buffer needle) {
        return indexOf(self, start, start+needle.length(), needle) == 0;
    }

    static public int indexOf(Buffer self, int start, Buffer needle) {
        return indexOf(self, start, self.length(), needle);
    }

    static public int indexOf(Buffer self, Buffer needle) {
        return indexOf(self, 0, self.length(), needle);
    }

    static public int indexOf(Buffer self, int start, int end, Buffer needle) {
        int max = Math.min(end, self.length() - needle.length());
        for (int i = start; i <= max; i++) {
            if (matches(self, i, needle)) {
                return i;
            }
        }
        return -1;
    }

    static public boolean matches(Buffer self, int pos, Buffer needle) {
        int needleLength = needle.length();
        for (int i = 0; i < needleLength; i++) {
            if( self.getByte(pos+i) != needle.getByte(i) ) {
                return false;
            }
        }
        return true;
    }

    static private final Field bufferField;
    static {
        try {
            bufferField = Buffer.class.getDeclaredField("buffer");
            bufferField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    static private ByteBuf getNettyByteBuf(Buffer self) {
        try {
            return (ByteBuf)bufferField.get(self);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static public void setLength(Buffer self, int length) {
        getNettyByteBuf(self).capacity(length);
    }

    static final public Buffer[] split(Buffer self, byte separator) {
        ArrayList<Buffer> rc = new ArrayList<Buffer>();
        int pos = 0;
        int nextStart = pos;
        int end = self.length();
        while( pos < end ) {
            if( self.getByte(pos)==separator ) {
                if( nextStart < pos ) {
                    rc.add(self.getBuffer(nextStart, pos));
                }
                nextStart = pos+1;
            }
            pos++;
        }
        if( nextStart < pos ) {
            rc.add(self.getBuffer(nextStart, pos));
        }
        return rc.toArray(new Buffer[rc.size()]);
    }


    public static Buffer toBuffer(ByteBuffer buff) {
        Buffer self = new Buffer(buff.remaining());
        while( buff.hasRemaining() ) {
            self.appendByte(buff.get());
        }
        return self;
    }

}
