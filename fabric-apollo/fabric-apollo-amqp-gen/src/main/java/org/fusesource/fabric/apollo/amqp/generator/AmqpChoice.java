/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.generator;

import org.fusesource.fusemq.amqp.jaxb.schema.Choice;

import java.util.LinkedList;

public class AmqpChoice {

    LinkedList<Choice> choices = new LinkedList<Choice>();
    public void parseFromChoice(Choice choice) {
        choices.add(choice);
    }
}
