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

package org.fusesource.fabric.apollo.amqp.codec.api;

import org.fusesource.fabric.apollo.amqp.codec.marshaller.MessageSupport;

/**
 *
 */
public class MessageFactory {

    public static DataMessage createDataMessage(Object... args) {
        return MessageSupport.createDataMessage(args);
    }

    public static SequenceMessage createSequenceMessage(Object... args) {
        return MessageSupport.createSequenceMessage(args);
    }

    public static ValueMessage createValueMessage(Object... args) {
        return MessageSupport.createValueMessage(args);
    }

    public static AnnotatedMessage createAnnotatedMessage(Object... args) {
        return MessageSupport.createAnnotatedMessage(args);
    }
}
