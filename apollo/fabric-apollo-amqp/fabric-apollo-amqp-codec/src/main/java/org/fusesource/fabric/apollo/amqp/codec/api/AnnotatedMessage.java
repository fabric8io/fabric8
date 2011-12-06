/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
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
