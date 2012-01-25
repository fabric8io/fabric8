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

package org.fusesource.fabric.apollo.amqp.codec.api;

import org.fusesource.fabric.apollo.amqp.codec.types.DeliveryAnnotations;
import org.fusesource.fabric.apollo.amqp.codec.types.Footer;
import org.fusesource.fabric.apollo.amqp.codec.types.Header;
import org.fusesource.fabric.apollo.amqp.codec.types.MessageAnnotations;

/**
 *
 */
public interface AnnotatedMessage {
    Header getHeader();

    void setHeader(Header header);

    DeliveryAnnotations getDeliveryAnnotations();

    void setDeliveryAnnotations(DeliveryAnnotations deliveryAnnotations);

    MessageAnnotations getMessageAnnotations();

    void setMessageAnnotations(MessageAnnotations messageAnnotations);

    BareMessage getMessage();

    void setMessage(BareMessage message);

    Footer getFooter();

    void setFooter(Footer footer);
}
