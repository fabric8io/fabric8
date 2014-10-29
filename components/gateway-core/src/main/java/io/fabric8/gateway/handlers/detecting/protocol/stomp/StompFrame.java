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
package io.fabric8.gateway.handlers.detecting.protocol.stomp;

import io.fabric8.gateway.handlers.detecting.protocol.Ascii;
import org.vertx.java.core.buffer.Buffer;

import java.io.UnsupportedEncodingException;
import java.util.*;

import static io.fabric8.gateway.handlers.detecting.protocol.Ascii.ascii;
import static io.fabric8.gateway.handlers.detecting.protocol.BufferSupport.startsWith;
import static io.fabric8.gateway.handlers.detecting.protocol.stomp.Constants.*;

/**
 * A STOMP protocol frame.
 *
 */
public class StompFrame {

    public static final Buffer NO_DATA = new Buffer(new byte[]{});

    static public class HeaderEntry {
        public final Ascii key;
        public final Ascii value;

        public HeaderEntry(Ascii key, Ascii value) {
            this.key = key;
            this.value = value;
        }

        public Ascii getKey() {
            return key;
        }

        public Ascii getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "" + key +
                    "=" + value;
        }
    }

    private Ascii action;
    private ArrayList<HeaderEntry> headerList;
    private HashMap<Ascii, Ascii> headerMap = new HashMap<Ascii, Ascii>(16);
    private Buffer content = NO_DATA;

    public StompFrame() {
    }

    public StompFrame(Ascii action) {
        this.action = action;
    }

    public StompFrame clone() {
        StompFrame rc = new StompFrame(action);
        if( headerList!=null ) {
            rc.headerList = new ArrayList<HeaderEntry>(headerList);
            rc.headerMap = null;
        } else {
            rc.headerMap = new HashMap<Ascii,Ascii>(headerMap);
            rc.headerList = null;
        }
        rc.content = content;
        return rc;
    }

    public Ascii action() {
        return action;
    }

    public StompFrame action(Ascii action) {
        assert action != null;
        this.action = action;
        return this;
    }

    public Buffer content() {
        return this.content;
    }

    public StompFrame content(Buffer content) {
        assert content != null;
        this.content = content;
        return this;
    }

    public String contentAsString() {
        return content.getString(0, content.length(), "UTF-8");
    }

    public Map<Ascii, Ascii> headerMap() {
        return headerMap(Collections.EMPTY_SET);
    }

    public Map<Ascii, Ascii> headerMap(Set<Ascii> reversedHeaderHandling) {
        if( headerMap==null ) {
            headerMap = new HashMap<Ascii, Ascii>();
            for (HeaderEntry HeaderEntry : headerList) {
                final Ascii key = HeaderEntry.getKey();
                Ascii old = headerMap.put(key, HeaderEntry.getValue());
                if( old !=null && !reversedHeaderHandling.contains(key) ) {
                    headerMap.put(key, old);
                }
            }
            headerList = null;
        }
        return headerMap;
    }

    public List<HeaderEntry> headerList() {
        if( headerList==null ) {
            for (Map.Entry<Ascii,Ascii> entry : headerMap.entrySet()) {
                headerList.add(new HeaderEntry(entry.getKey(), entry.getValue()));
            }
            headerMap = null;
        }
        return headerList;
    }

    public void addHeader(Ascii key, Ascii value) {
        if( headerList!=null ) {
            headerList.add(0, new HeaderEntry(key, value));
        } else {
            headerMap.put(key, value);
        }
    }

    String getHeaderAsString(Ascii key) {
        Ascii header = getHeader(key);
        if( header !=null ) {
            return decodeHeader(header.toBuffer());
        }
        return null;
    }

    public Ascii getHeader(Ascii key) {
        if( headerList!=null ) {
            for (HeaderEntry HeaderEntry : headerList) {
                if( HeaderEntry.getKey().equals(key) ) {
                    return HeaderEntry.getValue();
                }
            }
            return null;
        } else {
            return headerMap.get(key);
        }
    }

    public void clearHeaders() {
        if( headerList!=null) {
            headerList.clear();
        } else {
            headerMap.clear();
        }
    }

    public void setHeaders(ArrayList<HeaderEntry> values) {
        headerList = values;
        headerMap = null;
    }
    /*
    public Buffer toBuffer() {
        return toBuffer(true);
    }
    */

    /*
    public Buffer toBuffer(boolean includeBody) {
        try {
            DataByteArrayOutputStream out = new DataByteArrayOutputStream();
            write(out, includeBody);
            return out.toBuffer();
        } catch (IOException e) {
            throw new RuntimeException(e); // not expected to occur.
        }
    }
    */

    /*
    private void write(DataOutput out, Buffer buffer) throws IOException {
        out.write(buffer.data, buffer.offset, buffer.length);
    }

    public void write(DataOutput out) throws IOException {
        write(out, true);
    }
    */

    public void addContentLengthHeader() {
        addHeader(CONTENT_LENGTH, new Ascii(Integer.toString(content.length())));
    }
    /*
    public int size() {
        int rc = action.length() + 1;
        if( headerList!=null ) {
            for (HeaderEntry entry : headerList) {
                rc += entry.getKey().length() + entry.getValue().length() + 2;
            }
        } else {
            for (Map.Entry<Ascii,Ascii> entry : headerMap.entrySet()) {
                rc += entry.getKey().length() + entry.getValue().length() + 2;
            }
        }
        rc += content.length() + 3;
        return rc;
    }
    */

    /*
    public void write(DataOutput out, boolean includeBody) throws IOException {
        write(out, action);
        out.writeByte(NEWLINE_BYTE);

        if( headerList!=null ) {
            for (HeaderEntry entry : headerList) {
                write(out, entry.getKey());
                out.writeByte(COLON_BYTE);
                write(out, entry.getValue());
                out.writeByte(NEWLINE_BYTE);
            }
        } else {
            for (Map.Entry<Ascii,Ascii> entry : headerMap.entrySet()) {
                write(out, entry.getKey());
                out.writeByte(COLON_BYTE);
                write(out, entry.getValue());
                out.writeByte(NEWLINE_BYTE);
            }
        }

        //denotes end of headers with a new line
        out.writeByte(NEWLINE_BYTE);
        if (includeBody) {
            write(out, content);
            out.writeByte(NULL_BYTE);
            out.writeByte(NEWLINE_BYTE);
        }
    }
    */
    /*
    public String toString() {
        return toBuffer(false).ascii().toString();
    }

    public String errorMessage() {
        Ascii value = getHeader(MESSAGE_HEADER);
        if (value != null) {
            return decodeHeader(value);
        } else {
            return contentAsString();
        }
    }
    */

    public static String decodeHeader(Buffer value) {
        if (value == null)
            return null;

        Buffer rc = new Buffer(value.length());
        int pos = 0;
        int max = value.length();
        while (pos < max) {
            if (startsWith(value, pos, ESCAPE_ESCAPE_SEQ.toBuffer())) {
                rc.appendByte(ESCAPE_BYTE);
                pos += 2;
            } else if (startsWith(value, pos, COLON_ESCAPE_SEQ.toBuffer())) {
                rc.appendByte(COLON_BYTE);
                pos += 2;
            } else if (startsWith(value, pos, NEWLINE_ESCAPE_SEQ.toBuffer())) {
                rc.appendByte(NEWLINE_BYTE);
                pos += 2;
            } else {
                rc.appendByte(value.getByte(pos));
                pos += 1;
            }
        }
        return rc.toString();
    }


    public static Ascii encodeHeader(String value) {
        if (value == null)
            return null;
        try {
            byte[] data = value.getBytes("UTF-8");
            Buffer rc = new Buffer(data.length);
            for (byte d : data) {
                switch (d) {
                    case ESCAPE_BYTE:
                        rc.appendBuffer(ESCAPE_ESCAPE_SEQ.toBuffer());
                        break;
                    case COLON_BYTE:
                        rc.appendBuffer(COLON_ESCAPE_SEQ.toBuffer());
                        break;
                    case NEWLINE_BYTE:
                        rc.appendBuffer(COLON_ESCAPE_SEQ.toBuffer());
                        break;
                    default:
                        rc.appendByte(d);
                }
            }
            return ascii(rc);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // not expected.
        }
    }

    public static Map<Ascii, Ascii> encodeHeaders(Map<String, String> headers) {
        if(headers==null)
            return null;
        HashMap<Ascii, Ascii> rc = new HashMap<Ascii, Ascii>(headers.size());
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            rc.put(StompFrame.encodeHeader(entry.getKey()), StompFrame.encodeHeader(entry.getValue()));
        }
        return rc;
    }
}
