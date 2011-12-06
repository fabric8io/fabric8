/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.io;


import org.fusesource.fabric.dosgi.tcp.TcpTransport;

/**
 * Implemented by object that need to get injected by
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public interface TransportAcceptListener {
    
    void onAccept(TransportServer transportServer, TcpTransport transport);
    
    void onAcceptError(TransportServer transportServer, Exception error);

}
