/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.api;

import org.fusesource.fabric.apollo.amqp.codec.interfaces.Source;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.Target;
import org.fusesource.fabric.apollo.amqp.codec.types.Role;

/**
 *
 */
public interface Link {

    public String getName();

    public void setName(String name);

    public Role getRole();

    public void setMaxMessageSize(long size);

    public long getMaxMessageSize();

    public void setSource(Source source);

    public void setTarget(Target target);

    public Source getSource();

    public Target getTarget();

    public void onAttach(Runnable task);

    public void onDetach(Runnable task);

    public boolean established();

    public Session getSession();

}
