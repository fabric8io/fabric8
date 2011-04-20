/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.io;

public interface ServerInvoker extends Service {

    String getConnectAddress();

    void registerService(String id, ServiceFactory service, ClassLoader classLoader);

    void unregisterService(String id);


    public interface ServiceFactory {

        Object get();

        void unget();

    }
}
