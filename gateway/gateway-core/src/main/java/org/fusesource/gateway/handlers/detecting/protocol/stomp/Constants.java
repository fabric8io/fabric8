/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.gateway.handlers.detecting.protocol.stomp;


import org.fusesource.gateway.handlers.detecting.protocol.Ascii;
import static org.fusesource.gateway.handlers.detecting.protocol.Ascii.ascii;

/**
 * Holds STOMP related constants.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public interface Constants {

    final Ascii NULL = ascii("\u0000");
    final byte NULL_BYTE = 0;

    final Ascii NEWLINE = ascii("\n");
    final byte NEWLINE_BYTE = '\n';

    final Ascii COLON = ascii(":");
    final byte COLON_BYTE = ':';

    final byte ESCAPE_BYTE = '\\';

    final Ascii ESCAPE_ESCAPE_SEQ = ascii("\\\\");
    final Ascii COLON_ESCAPE_SEQ = ascii("\\c");
    final Ascii NEWLINE_ESCAPE_SEQ = ascii("\\n");

    // Commands
    final Ascii CONNECT = ascii("CONNECT");
    final Ascii STOMP = ascii("STOMP");
    final Ascii SEND = ascii("SEND");
    final Ascii DISCONNECT = ascii("DISCONNECT");
    final Ascii SUBSCRIBE = ascii("SUBSCRIBE");
    final Ascii UNSUBSCRIBE = ascii("UNSUBSCRIBE");
    final Ascii MESSAGE = ascii("MESSAGE");

    final Ascii BEGIN_TRANSACTION = ascii("BEGIN");
    final Ascii COMMIT_TRANSACTION = ascii("COMMIT");
    final Ascii ABORT_TRANSACTION = ascii("ABORT");
    final Ascii BEGIN = ascii("BEGIN");
    final Ascii COMMIT = ascii("COMMIT");
    final Ascii ABORT = ascii("ABORT");
    final Ascii ACK = ascii("ACK");

    // Responses
    final Ascii CONNECTED = ascii("CONNECTED");
    final Ascii ERROR = ascii("ERROR");
    final Ascii RECEIPT = ascii("RECEIPT");

    // Headers
    final Ascii RECEIPT_REQUESTED = ascii("receipt");
    final Ascii TRANSACTION = ascii("transaction");
    final Ascii CONTENT_LENGTH = ascii("content-length");
    final Ascii CONTENT_TYPE = ascii("content-type");
    final Ascii TRANSFORMATION = ascii("transformation");
    final Ascii TRANSFORMATION_ERROR = ascii("transformation-error");

    /**
     * This header is used to instruct ActiveMQ to construct the message
     * based with a specific type.
     */
    final Ascii AMQ_MESSAGE_TYPE = ascii("amq-msg-type");
    final Ascii RECEIPT_ID = ascii("receipt-id");
    final Ascii PERSISTENT = ascii("persistent");
    final Ascii MESSAGE_HEADER = ascii("message");
    final Ascii MESSAGE_ID = ascii("message-id");
    final Ascii CORRELATION_ID = ascii("correlation-id");
    final Ascii EXPIRATION_TIME = ascii("expires");
    final Ascii REPLY_TO = ascii("reply-to");
    final Ascii PRIORITY = ascii("priority");
    final Ascii REDELIVERED = ascii("redelivered");
    final Ascii TIMESTAMP = ascii("timestamp");
    final Ascii TYPE = ascii("type");
    final Ascii SUBSCRIPTION = ascii("subscription");
    final Ascii USERID = ascii("JMSXUserID");
    final Ascii PROPERTIES = ascii("JMSXProperties");
    final Ascii ACK_MODE = ascii("ack");
    final Ascii ID = ascii("id");
    final Ascii SELECTOR = ascii("selector");
    final Ascii BROWSER = ascii("browser");
    final Ascii AUTO = ascii("auto");
    final Ascii CLIENT = ascii("client");
    final Ascii INDIVIDUAL = ascii("client-individual");
    final Ascii DESTINATION = ascii("destination");
    final Ascii LOGIN = ascii("login");
    final Ascii PASSCODE = ascii("passcode");
    final Ascii CLIENT_ID = ascii("client-id");
    final Ascii REQUEST_ID = ascii("request-id");
    final Ascii SESSION = ascii("session");
    final Ascii RESPONSE_ID = ascii("response-id");
    final Ascii ACCEPT_VERSION = ascii("accept-version");
    final Ascii V1_1 = ascii("1.1");
    final Ascii V1_0 = ascii("1.0");
    final Ascii HOST = ascii("host");
    final Ascii TRUE = ascii("true");
    final Ascii FALSE = ascii("false");
    final Ascii END = ascii("end");
    final Ascii HOST_ID = ascii("host-id");
    final Ascii SERVER = ascii("server");
    final Ascii CREDIT = ascii("credit");
    final Ascii JMSX_DELIVERY_COUNT = ascii("JMSXDeliveryCount");


}
