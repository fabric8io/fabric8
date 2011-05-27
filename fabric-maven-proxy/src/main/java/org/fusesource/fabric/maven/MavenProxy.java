/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.maven;

import java.io.IOException;
import java.net.URI;

public interface MavenProxy {

    void start() throws IOException;

    void stop();

    boolean isStarted();

    URI getAddress();
}
