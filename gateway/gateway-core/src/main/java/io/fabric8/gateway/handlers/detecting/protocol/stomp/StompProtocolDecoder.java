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
import io.fabric8.gateway.handlers.detecting.protocol.ProtocolDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;

import java.io.IOException;
import java.util.ArrayList;

import static io.fabric8.gateway.handlers.detecting.protocol.Ascii.ascii;
import static io.fabric8.gateway.handlers.detecting.protocol.BufferSupport.chomp;
import static io.fabric8.gateway.handlers.detecting.protocol.BufferSupport.indexOf;
import static io.fabric8.gateway.handlers.detecting.protocol.BufferSupport.trim;
import static io.fabric8.gateway.handlers.detecting.protocol.stomp.Constants.COLON_BYTE;
import static io.fabric8.gateway.handlers.detecting.protocol.stomp.Constants.CONTENT_LENGTH;

/**
 * Implements protocol decoding for the STOMP protocol.
 */
class StompProtocolDecoder extends ProtocolDecoder<StompFrame> {

    private static final transient Logger LOG = LoggerFactory.getLogger(StompProtocolDecoder.class);

    private final StompProtocol protocol;
    public boolean trim = false;

    public StompProtocolDecoder(StompProtocol protocol) {
        this.protocol = protocol;
    }

    @Override
    protected Action<StompFrame> initialDecodeAction() {
        return read_action;
    }


    final Action<StompFrame> read_action = new Action<StompFrame>() {
        public StompFrame apply() throws IOException {
            Buffer line = readUntil((byte) '\n', StompProtocol.maxCommandLength, "The maximum command length was exceeded");
            if (line != null) {
                Buffer action = chomp(line);
                if (trim) {
                    action = trim(action);
                }
                if (action.length() > 0) {
                    StompFrame frame = new StompFrame(Ascii.ascii(action));
                    nextDecodeAction = read_headers(frame);
                    return nextDecodeAction.apply();
                }
            }
            return null;
        }
    };

    private Action<StompFrame> read_headers(final StompFrame frame) {
        final Ascii[] contentLengthValue = new Ascii[1];
        final ArrayList<StompFrame.HeaderEntry> headers = new ArrayList<StompFrame.HeaderEntry>(10);
        return new Action<StompFrame>() {
            public StompFrame apply() throws IOException {
                Buffer line = readUntil((byte) '\n', protocol.maxHeaderLength, "The maximum header length was exceeded");
                while (line != null) {
                    line = chomp(line);
                    if (line.length() > 0) {

                        if (protocol.maxHeaders != -1 && headers.size() > protocol.maxHeaders) {
                            throw new IOException("The maximum number of headers was exceeded");
                        }

                        try {
                            int seperatorIndex = indexOf(line, COLON_BYTE);
                            if (seperatorIndex < 0) {
                                throw new IOException("Header line missing separator [" + Ascii.ascii(line) + "]");
                            }
                            Buffer name = line.getBuffer(0, seperatorIndex);
                            if (trim) {
                                name = trim(name);
                            }

                            Buffer value = line.getBuffer(seperatorIndex + 1, line.length());
                            if (trim) {
                                value = trim(value);
                            }
                            StompFrame.HeaderEntry entry = new StompFrame.HeaderEntry(Ascii.ascii(name), Ascii.ascii(value));
                            if (entry.key.equals(CONTENT_LENGTH)) {
                                contentLengthValue[0] = entry.value;
                            }
                            headers.add(entry);
                        } catch (Exception e) {
                            throw new IOException("Unable to parser header line [" + line + "]");
                        }

                    } else {
                        frame.setHeaders(headers);
                        Ascii contentLength = contentLengthValue[0];
                        if (contentLength != null) {
                            // Bless the client, he's telling us how much data to read in.
                            int length = 0;
                            try {
                                length = Integer.parseInt(contentLength.toString());
                            } catch (NumberFormatException e) {
                                throw new IOException("Specified content-length is not a valid integer");
                            }

                            if (protocol.maxDataLength != -1 && length > protocol.maxDataLength) {
                                throw new IOException("The maximum data length was exceeded");
                            }

                            nextDecodeAction = read_binary_body(frame, length);
                        } else {
                            nextDecodeAction = read_text_body(frame);
                        }
                        return nextDecodeAction.apply();
                    }
                    line = readUntil((byte) '\n', protocol.maxHeaderLength, "The maximum header length was exceeded");
                }
                return null;
            }
        };
    }

    private Action<StompFrame> read_binary_body(final StompFrame frame, final int contentLength) {
        return new Action<StompFrame>() {
            public StompFrame apply() throws IOException {
                Buffer content = readBytes(contentLength + 1);
                if (content != null) {
                    if (content.getByte(contentLength) != 0) {
                        throw new IOException("Expected null terminator after " + contentLength + " content bytes");
                    }
                    frame.content(chomp(content));
                    nextDecodeAction = read_action;
                    return frame;
                } else {
                    return null;
                }
            }
        };
    }

    private Action<StompFrame> read_text_body(final StompFrame frame) {
        return new Action<StompFrame>() {
            public StompFrame apply() throws IOException {
                Buffer content = readUntil((byte) 0);
                if (content != null) {
                    nextDecodeAction = read_action;
                    frame.content(chomp(content));
                    return frame;
                } else {
                    return null;
                }
            }
        };
    }

}
