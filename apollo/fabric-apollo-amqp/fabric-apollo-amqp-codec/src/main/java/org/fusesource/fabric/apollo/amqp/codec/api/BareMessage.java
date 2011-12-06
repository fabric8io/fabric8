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

import org.fusesource.fabric.apollo.amqp.codec.types.ApplicationProperties;
import org.fusesource.fabric.apollo.amqp.codec.types.Properties;

/**
 *
 */
public interface BareMessage<K> {

    K getData();

    void setData(K data);

    Properties getProperties();

    Properties getProperties(boolean create);

    void setProperties(Properties properties);

    ApplicationProperties getApplicationProperties();

    ApplicationProperties getApplicationProperties(boolean create);

    void setApplicationProperties(ApplicationProperties applicationProperties);
}
