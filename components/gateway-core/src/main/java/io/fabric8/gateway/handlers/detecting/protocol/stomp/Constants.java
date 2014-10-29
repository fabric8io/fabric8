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

/**
 * Holds STOMP related constants.
 *
 */
public interface Constants {

    final Ascii NULL = Ascii.ascii("\u0000");
    final byte NULL_BYTE = 0;

    final Ascii NEWLINE = Ascii.ascii("\n");
    final byte NEWLINE_BYTE = '\n';

    final Ascii COLON = Ascii.ascii(":");
    final byte COLON_BYTE = ':';

    final byte ESCAPE_BYTE = '\\';

    final Ascii ESCAPE_ESCAPE_SEQ = Ascii.ascii("\\\\");
    final Ascii COLON_ESCAPE_SEQ = Ascii.ascii("\\c");
    final Ascii NEWLINE_ESCAPE_SEQ = Ascii.ascii("\\n");

    // Commands
    final Ascii CONNECT = Ascii.ascii("CONNECT");
    final Ascii STOMP = Ascii.ascii("STOMP");
    final Ascii SEND = Ascii.ascii("SEND");
    final Ascii DISCONNECT = Ascii.ascii("DISCONNECT");
    final Ascii SUBSCRIBE = Ascii.ascii("SUBSCRIBE");
    final Ascii UNSUBSCRIBE = Ascii.ascii("UNSUBSCRIBE");
    final Ascii MESSAGE = Ascii.ascii("MESSAGE");

    final Ascii BEGIN_TRANSACTION = Ascii.ascii("BEGIN");
    final Ascii COMMIT_TRANSACTION = Ascii.ascii("COMMIT");
    final Ascii ABORT_TRANSACTION = Ascii.ascii("ABORT");
    final Ascii BEGIN = Ascii.ascii("BEGIN");
    final Ascii COMMIT = Ascii.ascii("COMMIT");
    final Ascii ABORT = Ascii.ascii("ABORT");
    final Ascii ACK = Ascii.ascii("ACK");

    // Responses
    final Ascii CONNECTED = Ascii.ascii("CONNECTED");
    final Ascii ERROR = Ascii.ascii("ERROR");
    final Ascii RECEIPT = Ascii.ascii("RECEIPT");

    // Headers
    final Ascii RECEIPT_REQUESTED = Ascii.ascii("receipt");
    final Ascii TRANSACTION = Ascii.ascii("transaction");
    final Ascii CONTENT_LENGTH = Ascii.ascii("content-length");
    final Ascii CONTENT_TYPE = Ascii.ascii("content-type");
    final Ascii TRANSFORMATION = Ascii.ascii("transformation");
    final Ascii TRANSFORMATION_ERROR = Ascii.ascii("transformation-error");

    /**
     * This header is used to instruct ActiveMQ to construct the message
     * based with a specific type.
     */
    final Ascii AMQ_MESSAGE_TYPE = Ascii.ascii("amq-msg-type");
    final Ascii RECEIPT_ID = Ascii.ascii("receipt-id");
    final Ascii PERSISTENT = Ascii.ascii("persistent");
    final Ascii MESSAGE_HEADER = Ascii.ascii("message");
    final Ascii MESSAGE_ID = Ascii.ascii("message-id");
    final Ascii CORRELATION_ID = Ascii.ascii("correlation-id");
    final Ascii EXPIRATION_TIME = Ascii.ascii("expires");
    final Ascii REPLY_TO = Ascii.ascii("reply-to");
    final Ascii PRIORITY = Ascii.ascii("priority");
    final Ascii REDELIVERED = Ascii.ascii("redelivered");
    final Ascii TIMESTAMP = Ascii.ascii("timestamp");
    final Ascii TYPE = Ascii.ascii("type");
    final Ascii SUBSCRIPTION = Ascii.ascii("subscription");
    final Ascii USERID = Ascii.ascii("JMSXUserID");
    final Ascii PROPERTIES = Ascii.ascii("JMSXProperties");
    final Ascii ACK_MODE = Ascii.ascii("ack");
    final Ascii ID = Ascii.ascii("id");
    final Ascii SELECTOR = Ascii.ascii("selector");
    final Ascii BROWSER = Ascii.ascii("browser");
    final Ascii AUTO = Ascii.ascii("auto");
    final Ascii CLIENT = Ascii.ascii("client");
    final Ascii INDIVIDUAL = Ascii.ascii("client-individual");
    final Ascii DESTINATION = Ascii.ascii("destination");
    final Ascii LOGIN = Ascii.ascii("login");
    final Ascii PASSCODE = Ascii.ascii("passcode");
    final Ascii CLIENT_ID = Ascii.ascii("client-id");
    final Ascii REQUEST_ID = Ascii.ascii("request-id");
    final Ascii SESSION = Ascii.ascii("session");
    final Ascii RESPONSE_ID = Ascii.ascii("response-id");
    final Ascii ACCEPT_VERSION = Ascii.ascii("accept-version");
    final Ascii V1_1 = Ascii.ascii("1.1");
    final Ascii V1_0 = Ascii.ascii("1.0");
    final Ascii HOST = Ascii.ascii("host");
    final Ascii TRUE = Ascii.ascii("true");
    final Ascii FALSE = Ascii.ascii("false");
    final Ascii END = Ascii.ascii("end");
    final Ascii HOST_ID = Ascii.ascii("host-id");
    final Ascii SERVER = Ascii.ascii("server");
    final Ascii CREDIT = Ascii.ascii("credit");
    final Ascii JMSX_DELIVERY_COUNT = Ascii.ascii("JMSXDeliveryCount");


}
